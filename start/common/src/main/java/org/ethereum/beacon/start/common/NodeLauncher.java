package org.ethereum.beacon.start.common;

import static org.ethereum.beacon.chain.observer.ObservableStateProcessorImpl.DEFAULT_EMPTY_SLOT_TRANSITIONS_LIMIT;

import java.time.Duration;
import java.util.List;
import org.ethereum.beacon.chain.DefaultBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.ProposedBlockProcessor;
import org.ethereum.beacon.chain.ProposedBlockProcessorImpl;
import org.ethereum.beacon.chain.SlotTicker;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.ObservableStateProcessorImpl;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.MultiValidatorService;
import org.ethereum.beacon.validator.attester.BeaconChainAttesterImpl;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.validator.proposer.BeaconChainProposerImpl;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.MessageSerializer;
import org.ethereum.beacon.wire.SimplePeerManagerImpl;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.WireApiSyncServer;
import org.ethereum.beacon.wire.message.SSZMessageSerializer;
import org.ethereum.beacon.wire.net.ConnectionManager;
import org.ethereum.beacon.wire.sync.BeaconBlockTree;
import org.ethereum.beacon.wire.sync.SyncManagerImpl;
import org.ethereum.beacon.wire.sync.SyncQueue;
import org.ethereum.beacon.wire.sync.SyncQueueImpl;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.util.uint.UInt64;

public class NodeLauncher {
  private final BeaconChainSpec spec;
  private final DepositContract depositContract;
  private final List<BLS381Credentials> validatorCred;
  private final BeaconChainStorageFactory storageFactory;
  private final Schedulers schedulers;

  private InitialStateTransition initialTransition;
  private PerSlotTransition perSlotTransition;
  private PerBlockTransition perBlockTransition;
  private PerEpochTransition perEpochTransition;
  private ExtendedSlotTransition extendedSlotTransition;
  private EmptySlotTransition emptySlotTransition;
  private BeaconBlockVerifier blockVerifier;
  private BeaconStateVerifier stateVerifier;

  private InMemoryDatabase db;
  private BeaconChainStorage beaconChainStorage;
  private MutableBeaconChain beaconChain;
  private SlotTicker slotTicker;
  private ObservableStateProcessor observableStateProcessor;
  private BeaconChainProposer beaconChainProposer;
  private BeaconChainAttesterImpl beaconChainAttester;
  private MultiValidatorService beaconChainValidator;

  private byte networkId = 1;
  private UInt64 chainId = UInt64.valueOf(1);
  private boolean startSyncManager = false;

  private WireApiSub wireApiSub;
  private WireApiSync wireApiSyncRemote;
  private final ConnectionManager<?> connectionManager;
  private SimplePeerManagerImpl peerManager;
  private BeaconBlockTree blockTree;
  private SyncQueue syncQueue;
  private SyncManagerImpl syncManager;
  private WireApiSyncServer syncServer;

  public NodeLauncher(
      BeaconChainSpec spec,
      DepositContract depositContract,
      List<BLS381Credentials> validatorCred,
      ConnectionManager<?> connectionManager,
      BeaconChainStorageFactory storageFactory,
      Schedulers schedulers,
      boolean startSyncManager) {

    this.spec = spec;
    this.depositContract = depositContract;
    this.validatorCred = validatorCred;
    this.connectionManager = connectionManager;
    this.storageFactory = storageFactory;
    this.schedulers = schedulers;
    this.startSyncManager = startSyncManager;

    if (depositContract != null) {
      Mono.from(depositContract.getChainStartMono()).subscribe(this::chainStarted);
    }
  }

  void chainStarted(ChainStart chainStartEvent) {
    initialTransition = new InitialStateTransition(chainStartEvent, spec);
    perSlotTransition = new PerSlotTransition(spec);
    perBlockTransition = new PerBlockTransition(spec);
    perEpochTransition = new PerEpochTransition(spec);
    extendedSlotTransition =
        new ExtendedSlotTransition(perEpochTransition, perSlotTransition, spec);
    emptySlotTransition = new EmptySlotTransition(extendedSlotTransition);

    db = new InMemoryDatabase();
    beaconChainStorage = storageFactory.create(db);

    blockVerifier = BeaconBlockVerifier.createDefault(spec);
    stateVerifier = BeaconStateVerifier.createDefault(spec);

    beaconChain =
        new DefaultBeaconChain(
            spec,
            initialTransition,
            emptySlotTransition,
            perBlockTransition,
            blockVerifier,
            stateVerifier,
            beaconChainStorage,
            schedulers);
    beaconChain.init();

    slotTicker =
        new SlotTicker(spec, beaconChain.getRecentlyProcessed().getState(), schedulers);
    slotTicker.start();

    DirectProcessor<Attestation> allAttestations = DirectProcessor.create();


    observableStateProcessor = new ObservableStateProcessorImpl(
        beaconChainStorage,
        slotTicker.getTickerStream(),
        allAttestations,
        beaconChain.getBlockStatesStream(),
        spec,
        emptySlotTransition,
        schedulers,
        validatorCred != null ? Integer.MAX_VALUE : DEFAULT_EMPTY_SLOT_TRANSITIONS_LIMIT);
    observableStateProcessor.start();

    SSZSerializer ssz = new SSZBuilder().buildSerializer();
    MessageSerializer messageSerializer = new SSZMessageSerializer(ssz);
    syncServer = new WireApiSyncServer(beaconChainStorage);

    peerManager = new SimplePeerManagerImpl(
        networkId,
        chainId,
        connectionManager.channelsStream(),
        ssz,
        spec,
        messageSerializer,
        schedulers,
        syncServer,
        beaconChain.getBlockStatesStream());

    wireApiSub = peerManager.getWireApiSub();
    wireApiSyncRemote = peerManager.getWireApiSync();

    blockTree = new BeaconBlockTree(spec.getObjectHasher());
    syncQueue = new SyncQueueImpl(blockTree);

    Flux<BeaconBlock> ownBlocks = Flux.empty();
    if (validatorCred != null) {
      beaconChainProposer = new BeaconChainProposerImpl(spec, perBlockTransition, depositContract);
      beaconChainAttester = new BeaconChainAttesterImpl(spec);

      beaconChainValidator = new MultiValidatorService(
          validatorCred,
          beaconChainProposer,
          beaconChainAttester,
          spec,
          observableStateProcessor.getObservableStateStream(),
          schedulers);
      beaconChainValidator.start();

      ProposedBlockProcessor proposedBlocksProcessor = new ProposedBlockProcessorImpl(
          beaconChain, schedulers);
      Flux.from(beaconChainValidator.getProposedBlocksStream())
          .subscribe(proposedBlocksProcessor::newBlockProposed);
      Flux.from(proposedBlocksProcessor.processedBlocksStream())
          .subscribe(wireApiSub::sendProposedBlock);

      Flux.from(beaconChainValidator.getAttestationsStream()).subscribe(wireApiSub::sendAttestation);
      Flux.from(beaconChainValidator.getAttestationsStream()).subscribe(allAttestations);

      ownBlocks = Flux.from(proposedBlocksProcessor.processedBlocksStream());
    }

    Flux.from(wireApiSub.inboundAttestationsStream())
        .publishOn(schedulers.events().toReactor())
        .subscribe(allAttestations);

    Flux<BeaconBlock> allNewBlocks = Flux.merge(ownBlocks, wireApiSub.inboundBlocksStream());
    syncManager = new SyncManagerImpl(
        beaconChain,
        allNewBlocks.map(Feedback::of),
        beaconChainStorage,
        spec,
        wireApiSyncRemote,
        syncQueue,
        1,
        schedulers.events());
    syncManager.setRequestsDelay(Duration.ofSeconds(1), Duration.ofSeconds(5));

    if (startSyncManager) {
      syncManager.start();
    }

//    Flux.from(wireApiSub.inboundBlocksStream())
//        .publishOn(schedulers.reactorEvents())
//        .subscribe(beaconChain::insert);
  }


  public BeaconChainSpec getSpec() {
    return spec;
  }

  public DepositContract getDepositContract() {
    return depositContract;
  }

  public List<BLS381Credentials> getValidatorCred() {
    return validatorCred;
  }

  public WireApiSub getWireApiSub() {
    return wireApiSub;
  }

  public InitialStateTransition getInitialTransition() {
    return initialTransition;
  }

  public PerSlotTransition getPerSlotTransition() {
    return perSlotTransition;
  }

  public PerBlockTransition getPerBlockTransition() {
    return perBlockTransition;
  }

  public PerEpochTransition getPerEpochTransition() {
    return perEpochTransition;
  }

  public ExtendedSlotTransition getExtendedSlotTransition() {
    return extendedSlotTransition;
  }

  public BeaconBlockVerifier getBlockVerifier() {
    return blockVerifier;
  }

  public BeaconStateVerifier getStateVerifier() {
    return stateVerifier;
  }

  public InMemoryDatabase getDb() {
    return db;
  }

  public BeaconChainStorage getBeaconChainStorage() {
    return beaconChainStorage;
  }

  public MutableBeaconChain getBeaconChain() {
    return beaconChain;
  }

  public SlotTicker getSlotTicker() {
    return slotTicker;
  }

  public ObservableStateProcessor getObservableStateProcessor() {
    return observableStateProcessor;
  }

  public BeaconChainProposer getBeaconChainProposer() {
    return beaconChainProposer;
  }

  public BeaconChainAttesterImpl getBeaconChainAttester() {
    return beaconChainAttester;
  }

  public MultiValidatorService getValidatorService() {
    return beaconChainValidator;
  }

  public BeaconChainStorageFactory getStorageFactory() {
    return storageFactory;
  }

  public Schedulers getSchedulers() {
    return schedulers;
  }

  public SyncManagerImpl getSyncManager() {
    return syncManager;
  }
}

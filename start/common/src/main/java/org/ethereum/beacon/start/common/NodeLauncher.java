package org.ethereum.beacon.start.common;

import static org.ethereum.beacon.chain.observer.ObservableStateProcessorImpl.DEFAULT_EMPTY_SLOT_TRANSITIONS_LIMIT;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.DefaultBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.ProposedBlockProcessor;
import org.ethereum.beacon.chain.ProposedBlockProcessorImpl;
import org.ethereum.beacon.chain.SlotTicker;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.ObservableStateProcessorImpl;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.spec.SpecConstantsResolver;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.node.metrics.Metrics;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.attester.BeaconChainAttesterImpl;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.validator.local.MultiValidatorService;
import org.ethereum.beacon.validator.proposer.BeaconChainProposerImpl;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.PeerManager;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.WireApiSyncServer;
import org.ethereum.beacon.wire.impl.libp2p.Libp2pLauncher;
import org.ethereum.beacon.wire.sync.BeaconBlockTree;
import org.ethereum.beacon.wire.sync.SyncManagerImpl;
import org.ethereum.beacon.wire.sync.SyncQueue;
import org.ethereum.beacon.wire.sync.SyncQueueImpl;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.uint.UInt64;

public class NodeLauncher {
  private static final Logger logger = LogManager.getLogger(NodeLauncher.class);

  private final BeaconChainSpec spec;
  private final DepositContract depositContract;
  private final List<BLS381Credentials> validatorCred;
  private final Schedulers schedulers;

  private PerSlotTransition perSlotTransition;
  private PerBlockTransition perBlockTransition;
  private PerEpochTransition perEpochTransition;
  private ExtendedSlotTransition extendedSlotTransition;
  private EmptySlotTransition emptySlotTransition;
  private BeaconBlockVerifier blockVerifier;
  private BeaconStateVerifier stateVerifier;

  private Database db;
  private BeaconChainStorage beaconChainStorage;
  private MutableBeaconChain beaconChain;
  private SlotTicker slotTicker;
  private ObservableStateProcessor observableStateProcessor;
  private BeaconChainProposer beaconChainProposer;
  private BeaconChainAttesterImpl beaconChainAttester;
  private MultiValidatorService beaconChainValidator;

  private byte networkId = 1;
  private UInt64 chainId = UInt64.valueOf(1);
  private Bytes4 fork = Bytes4.ZERO;
  private boolean startSyncManager = false;

  private WireApiSub wireApiSub;
  private WireApiSync wireApiSyncRemote;
  private final Libp2pLauncher networkLauncher;
  private PeerManager peerManager;
  private BeaconBlockTree blockTree;
  private SyncQueue syncQueue;
  private SyncManagerImpl syncManager;
  private WireApiSyncServer syncServer;

  public NodeLauncher(
      BeaconChainSpec spec,
      DepositContract depositContract,
      List<BLS381Credentials> validatorCred,
      Libp2pLauncher networkLauncher,
      Database db,
      BeaconChainStorage beaconChainStorage,
      Schedulers schedulers,
      boolean startSyncManager) {

    this.spec = spec;
    this.depositContract = depositContract;
    this.validatorCred = validatorCred;
    this.networkLauncher = networkLauncher;
    this.db = db;
    this.beaconChainStorage = beaconChainStorage;
    this.schedulers = schedulers;
    this.startSyncManager = startSyncManager;
  }

  public void start() {
    if (depositContract != null) {
      Mono.from(depositContract.getChainStartMono()).subscribe(this::chainStarted);
    }
  }

  void chainStarted(ChainStart chainStartEvent) {
    perSlotTransition = new PerSlotTransition(spec);
    perBlockTransition = new PerBlockTransition(spec);
    perEpochTransition = new PerEpochTransition(spec);
    extendedSlotTransition =
        new ExtendedSlotTransition(perEpochTransition, perSlotTransition, spec);
    emptySlotTransition = new EmptySlotTransition(extendedSlotTransition);

    blockVerifier = BeaconBlockVerifier.createDefault(spec);
    stateVerifier = BeaconStateVerifier.createDefault(spec);

    beaconChain =
        new DefaultBeaconChain(
            spec,
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

    Flux.from(observableStateProcessor.getObservableStateStream())
        .subscribe(
            obs -> {
              Metrics.onNewState(spec, obs);
            });
    observableStateProcessor.start();

    SSZSerializer ssz = new SSZBuilder()
        .withExternalVarResolver(new SpecConstantsResolver(spec.getConstants()))
        .withExtraObjectCreator(SpecConstants.class, spec.getConstants())
        .buildSerializer();
    syncServer = new WireApiSyncServer(beaconChainStorage);

    networkLauncher.setSpec(spec);
    networkLauncher.setSszSerializer(ssz);
    networkLauncher.setSchedulers(schedulers);
    networkLauncher.setWireApiSyncServer(syncServer);
    networkLauncher.setHeadStream(beaconChain.getBlockStatesStream());
    networkLauncher.setFork(fork);

    networkLauncher.init();
    peerManager = networkLauncher.getPeerManager();

    wireApiSub = peerManager.getWireApiSub();
    wireApiSyncRemote = peerManager.getWireApiSync();

    Flux.from(wireApiSub.inboundAttestationsStream())
        .subscribe(
            a -> {
              Metrics.attestationPropagated(a);
            });

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
        schedulers);
    syncManager.setRequestsDelay(Duration.ofSeconds(1), Duration.ofSeconds(5));

    if (startSyncManager) {
      syncManager.start();
    }

    try {
      networkLauncher.start().get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      logger.error("Problem with starting network", e);
    }
  }

  public void stop() {
    db.close();
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

  public Database getDb() {
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

  public Schedulers getSchedulers() {
    return schedulers;
  }

  public SyncManagerImpl getSyncManager() {
    return syncManager;
  }
}

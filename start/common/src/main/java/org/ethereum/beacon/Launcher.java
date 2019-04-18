package org.ethereum.beacon;

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
import org.ethereum.beacon.consensus.transition.StateCachingTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.util.stats.TimeCollector;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.MultiValidatorService;
import org.ethereum.beacon.validator.attester.BeaconChainAttesterImpl;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.validator.proposer.BeaconChainProposerImpl;
import org.ethereum.beacon.wire.WireApiSub;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Launcher {
  private final BeaconChainSpec spec;
  private final DepositContract depositContract;
  private final List<BLS381Credentials> validatorCred;
  private final WireApiSub wireApi;
  private final BeaconChainStorageFactory storageFactory;
  private final Schedulers schedulers;

  private InitialStateTransition initialTransition;
  private StateCachingTransition stateCachingTransition;
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

  private TimeCollector proposeTimeCollector;

  public Launcher(
      BeaconChainSpec spec,
      DepositContract depositContract,
      List<BLS381Credentials> validatorCred,
      WireApiSub wireApi,
      BeaconChainStorageFactory storageFactory,
      Schedulers schedulers) {
    this(spec, depositContract, validatorCred, wireApi, storageFactory, schedulers, new TimeCollector());
  }

  public Launcher(
      BeaconChainSpec spec,
      DepositContract depositContract,
      List<BLS381Credentials> validatorCred,
      WireApiSub wireApi,
      BeaconChainStorageFactory storageFactory,
      Schedulers schedulers,
      TimeCollector proposeTimeCollector) {

    this.spec = spec;
    this.depositContract = depositContract;
    this.validatorCred = validatorCred;
    this.wireApi = wireApi;
    this.storageFactory = storageFactory;
    this.schedulers = schedulers;
    this.proposeTimeCollector = proposeTimeCollector;

    if (depositContract != null) {
      Mono.from(depositContract.getChainStartMono()).subscribe(this::chainStarted);
    }
  }

  void chainStarted(ChainStart chainStartEvent) {
    initialTransition = new InitialStateTransition(chainStartEvent, spec);
    stateCachingTransition = new StateCachingTransition(spec);
    perSlotTransition = new PerSlotTransition(spec);
    perBlockTransition = new PerBlockTransition(spec);
    perEpochTransition = new PerEpochTransition(spec);
    extendedSlotTransition =
        new ExtendedSlotTransition(
            stateCachingTransition, perEpochTransition, perSlotTransition, spec);
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
    Flux.from(wireApi.inboundAttestationsStream())
        .publishOn(schedulers.reactorEvents())
        .subscribe(allAttestations);

    observableStateProcessor = new ObservableStateProcessorImpl(
        beaconChainStorage,
        slotTicker.getTickerStream(),
        allAttestations,
        beaconChain.getBlockStatesStream(),
        spec,
        emptySlotTransition,
        schedulers);
    observableStateProcessor.start();

    if (validatorCred != null) {
      beaconChainProposer = new BeaconChainProposerImpl(spec, perBlockTransition, depositContract);
      beaconChainAttester = new BeaconChainAttesterImpl(spec);

      beaconChainValidator = new MultiValidatorService(
          validatorCred,
          beaconChainProposer,
          beaconChainAttester,
          spec,
          observableStateProcessor.getObservableStateStream(),
          schedulers,
          proposeTimeCollector);
      beaconChainValidator.start();

      ProposedBlockProcessor proposedBlocksProcessor = new ProposedBlockProcessorImpl(
          beaconChain, schedulers);
      Flux.from(beaconChainValidator.getProposedBlocksStream())
          .subscribe(proposedBlocksProcessor::newBlockProposed);
      Flux.from(proposedBlocksProcessor.processedBlocksStream())
          .subscribe(wireApi::sendProposedBlock);

      Flux.from(beaconChainValidator.getAttestationsStream()).subscribe(wireApi::sendAttestation);
      Flux.from(beaconChainValidator.getAttestationsStream()).subscribe(allAttestations);
    }

    Flux.from(wireApi.inboundBlocksStream())
        .publishOn(schedulers.reactorEvents())
        .subscribe(beaconChain::insert);
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

  public WireApiSub getWireApi() {
    return wireApi;
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
}

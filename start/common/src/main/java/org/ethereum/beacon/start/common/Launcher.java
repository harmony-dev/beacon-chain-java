package org.ethereum.beacon.start.common;

import java.util.List;
import org.ethereum.beacon.bench.BenchmarkController;
import org.ethereum.beacon.bench.BenchmarkController.BenchmarkRoutine;
import org.ethereum.beacon.chain.BeaconDataProcessor;
import org.ethereum.beacon.chain.BeaconDataProcessorImpl;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.DefaultBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.ProposedBlockProcessor;
import org.ethereum.beacon.chain.ProposedBlockProcessorImpl;
import org.ethereum.beacon.chain.SlotTicker;
import org.ethereum.beacon.chain.TimeTicker;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.AttestationProduced;
import org.ethereum.beacon.chain.eventbus.events.AttestationReceived;
import org.ethereum.beacon.chain.eventbus.events.StateThroughoutSlotYielded;
import org.ethereum.beacon.chain.eventbus.events.BlockImported;
import org.ethereum.beacon.chain.eventbus.events.BlockProposed;
import org.ethereum.beacon.chain.eventbus.events.BlockReceived;
import org.ethereum.beacon.chain.eventbus.events.ChainStarted;
import org.ethereum.beacon.chain.eventbus.events.ProposedBlockImported;
import org.ethereum.beacon.chain.eventbus.events.ProposerStateYielded;
import org.ethereum.beacon.chain.eventbus.events.SlotTick;
import org.ethereum.beacon.chain.eventbus.events.TimeTick;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.ObservableStateProcessorImpl;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.util.StorageUtils;
import org.ethereum.beacon.chain.store.TransactionalStore;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.consensus.spec.ForkChoice;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.util.stats.MeasurementsCollector;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.attester.BeaconChainAttesterImpl;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.validator.local.MultiValidatorService;
import org.ethereum.beacon.validator.proposer.BeaconChainProposerImpl;
import org.ethereum.beacon.wire.WireApiSub;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

public class Launcher {
  private final BeaconChainSpec spec;
  private final DepositContract depositContract;
  private final List<BLS381Credentials> validatorCred;
  private final WireApiSub wireApi;
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
  private Publisher<SignedBeaconBlock> importedBlockStream;
  private SlotTicker slotTicker;
  private Publisher<SlotNumber> slotStream;
  private ObservableStateProcessor observableStateProcessor;
  private Publisher<ObservableBeaconState> observableStateStream;
  private BeaconChainProposer beaconChainProposer;
  private BeaconChainAttesterImpl beaconChainAttester;
  private MultiValidatorService beaconChainValidator;

  private BenchmarkController benchmarkController;

  private MeasurementsCollector slotCollector = new MeasurementsCollector();
  private MeasurementsCollector epochCollector = new MeasurementsCollector();
  private MeasurementsCollector blockCollector = new MeasurementsCollector();

  private TimeTicker timeTicker;
  private TransactionalStore store;
  private BeaconDataProcessor beaconDataProcessor;
  private EventBus eventBus;

  public Launcher(
      BeaconChainSpec spec,
      DepositContract depositContract,
      List<BLS381Credentials> validatorCred,
      WireApiSub wireApi,
      BeaconChainStorageFactory storageFactory,
      Schedulers schedulers) {
    this(spec, depositContract, validatorCred, wireApi, storageFactory, schedulers,
        BenchmarkController.NO_BENCHES, false);
  }

  public Launcher(
      BeaconChainSpec spec,
      DepositContract depositContract,
      List<BLS381Credentials> validatorCred,
      WireApiSub wireApi,
      BeaconChainStorageFactory storageFactory,
      Schedulers schedulers,
      BenchmarkController benchmarkController) {
    this(spec, depositContract, validatorCred, wireApi, storageFactory, schedulers,
        benchmarkController, false);
  }

  public Launcher(
      BeaconChainSpec spec,
      DepositContract depositContract,
      List<BLS381Credentials> validatorCred,
      WireApiSub wireApi,
      BeaconChainStorageFactory storageFactory,
      Schedulers schedulers,
      BenchmarkController benchmarkController,
      boolean newDataProcessor) {

    this.spec = spec;
    this.depositContract = depositContract;
    this.validatorCred = validatorCred;
    this.wireApi = wireApi;
    this.storageFactory = storageFactory;
    this.schedulers = schedulers;
    this.benchmarkController = benchmarkController;

    if (depositContract != null) {
      if (newDataProcessor) {
        init();
        Mono.from(depositContract.getChainStartMono())
            .subscribe(chainStart -> eventBus.publish(ChainStarted.wrap(chainStart)));
      } else {
        Mono.from(depositContract.getChainStartMono()).subscribe(this::chainStarted);
      }
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
    BeaconStateEx initialState = initialTransition.apply(spec.get_empty_block());
    StorageUtils.initializeStorage(beaconChainStorage, spec, initialState);

    // do not create block verifier for benchmarks, otherwise verification won't be tracked by
    // controller
    blockVerifier =
        isBenchmarkMode()
            ? (block, state) -> VerificationResult.PASSED
            : BeaconBlockVerifier.createDefault(spec);
    stateVerifier = BeaconStateVerifier.createDefault(spec);

    beaconChain =
        new DefaultBeaconChain(
            spec,
            isBenchmarkMode() ? benchmarkingEmptySlotTransition(spec) : emptySlotTransition,
            isBenchmarkMode() ? benchmarkingBlockTransition(spec) : new PerBlockTransition(spec),
            blockVerifier,
            stateVerifier,
            beaconChainStorage,
            schedulers);
    importedBlockStream = Flux.from(beaconChain.getBlockStatesStream()).map(BeaconTuple::getSignedBlock);
    beaconChain.init();

    slotTicker =
        new SlotTicker(spec, beaconChain.getRecentlyProcessed().getState(), schedulers);
    slotStream = slotTicker.getTickerStream();
    slotTicker.start();

    DirectProcessor<Attestation> allAttestations = DirectProcessor.create();
    Flux.from(wireApi.inboundAttestationsStream())
        .publishOn(schedulers.events().toReactor())
        .subscribe(allAttestations);

    observableStateProcessor = new ObservableStateProcessorImpl(
        beaconChainStorage,
        slotTicker.getTickerStream(),
        allAttestations,
        beaconChain.getBlockStatesStream(),
        spec,
        emptySlotTransition,
        schedulers);
    observableStateStream = observableStateProcessor.getObservableStateStream();
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
          schedulers);
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
        .publishOn(schedulers.events().toReactor())
        .subscribe(beaconChain::insert);
  }

  void init() {
    timeTicker = new TimeTicker(schedulers);
    timeTicker.start();
    store = TransactionalStore.inMemoryStore();
    eventBus = EventBus.create(schedulers);
    beaconDataProcessor = new BeaconDataProcessorImpl(spec, store, eventBus);

    DirectProcessor<SlotNumber> slotStreamProcessor = DirectProcessor.create();
    FluxSink<SlotNumber> slotSink = slotStreamProcessor.sink();
    slotStream = slotStreamProcessor;
    eventBus.subscribe(SlotTick.class, slotSink::next);

    perSlotTransition = new PerSlotTransition(spec);
    perBlockTransition = new PerBlockTransition(spec);
    perEpochTransition = new PerEpochTransition(spec);
    extendedSlotTransition =
        new ExtendedSlotTransition(perEpochTransition, perSlotTransition, spec);

    DirectProcessor<ObservableBeaconState> stateFlux = DirectProcessor.create();
    stateFlux.publishOn(schedulers.events().toReactor());
    DirectProcessor<SignedBeaconBlock> importedBlockFlux = DirectProcessor.create();
    importedBlockFlux.publishOn(schedulers.events().toReactor());

    observableStateStream = stateFlux;
    importedBlockStream = importedBlockFlux;

    eventBus.subscribe(ProposerStateYielded.class, stateFlux::onNext);
    eventBus.subscribe(StateThroughoutSlotYielded.class, stateFlux::onNext);
    eventBus.subscribe(BlockImported.class, importedBlockFlux::onNext);

    if (validatorCred != null) {
      beaconChainProposer = new BeaconChainProposerImpl(spec, perBlockTransition, depositContract);
      beaconChainAttester = new BeaconChainAttesterImpl(spec);

      beaconChainValidator = new MultiValidatorService(
          validatorCred,
          beaconChainProposer,
          beaconChainAttester,
          spec,
          observableStateStream,
          schedulers);
      beaconChainValidator.start();

      Flux.from(beaconChainValidator.getProposedBlocksStream())
          .map(BlockProposed::wrap)
          .publishOn(schedulers.events().toReactor())
          .subscribe(eventBus::publish);
      eventBus.subscribe(ProposedBlockImported.class, wireApi::sendProposedBlock);

      Flux.from(beaconChainValidator.getAttestationsStream())
          .publishOn(schedulers.events().toReactor())
          .subscribe(wireApi::sendAttestation);
      Flux.from(beaconChainValidator.getAttestationsStream())
          .map(AttestationProduced::wrap)
          .publishOn(schedulers.events().toReactor())
          .subscribe(eventBus::publish);
    }

    Flux.from(wireApi.inboundBlocksStream())
        .map(BlockReceived::wrap)
        .publishOn(schedulers.events().toReactor())
        .subscribe(eventBus::publish);

    Flux.from(wireApi.inboundAttestationsStream())
        .map(AttestationReceived::wrap)
        .publishOn(schedulers.events().toReactor())
        .subscribe(eventBus::publish);

    Flux.from(timeTicker.getTickerStream())
        .map(TimeTick::wrap)
        .publishOn(schedulers.events().toReactor())
        .subscribe(eventBus::publish);
  }

  private EmptySlotTransition benchmarkingEmptySlotTransition(BeaconChainSpec spec) {
    BeaconChainSpec slotBench = benchmarkController.wrap(BenchmarkRoutine.SLOT, spec);
    BeaconChainSpec epochBench = benchmarkController.wrap(BenchmarkRoutine.EPOCH, spec);

    PerSlotTransition perSlotTransition = new PerSlotTransition(slotBench);
    PerEpochTransition perEpochTransition = new PerEpochTransition(epochBench);

    SlotNumber startSlot =
        spec.getConstants()
            .getGenesisSlot()
            .plus(benchmarkController.getWarmUpEpochs().mul(spec.getConstants().getSlotsPerEpoch()));
    ExtendedSlotTransition extendedSlotTransition =
        new ExtendedSlotTransition(perEpochTransition, perSlotTransition, spec) {
          @Override
          public BeaconStateEx apply(BeaconStateEx source) {
            long s = System.nanoTime();
            BeaconStateEx result = super.apply(source);
            long time = System.nanoTime() - s;
            if (result
                .getSlot()
                .modulo(spec.getConstants().getSlotsPerEpoch())
                .equals(SlotNumber.ZERO)) {
              if (result.getSlot().greater(startSlot)) {
                epochCollector.tick(time);
              }
            } else {
              if (result.getSlot().greaterEqual(startSlot)) {
                slotCollector.tick(time);
              }
            }
            return result;
          }
        };
    return new EmptySlotTransition(extendedSlotTransition);
  }

  private PerBlockTransition benchmarkingBlockTransition(BeaconChainSpec spec) {
    BeaconChainSpec blockBench = benchmarkController.wrap(BenchmarkRoutine.BLOCK, spec);
    SlotNumber startSlot =
        spec.getConstants()
            .getGenesisSlot()
            .plus(benchmarkController.getWarmUpEpochs().mul(spec.getConstants().getSlotsPerEpoch()));
    return new PerBlockTransition(blockBench) {
      @Override
      public BeaconStateEx apply(BeaconStateEx stateEx, BeaconBlock block) {
        long s = System.nanoTime();
        BeaconStateEx result = super.apply(stateEx, block);
        long time = System.nanoTime() - s;
        if (result.getSlot().greaterEqual(startSlot)) {
          blockCollector.tick(time);
        }
        return result;
      }
    };
  }

  private boolean isBenchmarkMode() {
    return benchmarkController != BenchmarkController.NO_BENCHES;
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

  public Publisher<SignedBeaconBlock> getImportedBlockStream() {
    return importedBlockStream;
  }

  public SlotTicker getSlotTicker() {
    return slotTicker;
  }

  public Publisher<SlotNumber> getSlotStream() {
    return slotStream;
  }

  public ObservableStateProcessor getObservableStateProcessor() {
    return observableStateProcessor;
  }

  public Publisher<ObservableBeaconState> getObservableStateStream() {
    return observableStateStream;
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

  public MeasurementsCollector getSlotCollector() {
    return slotCollector;
  }

  public MeasurementsCollector getEpochCollector() {
    return epochCollector;
  }

  public MeasurementsCollector getBlockCollector() {
    return blockCollector;
  }

  public ForkChoice.Store getStore() {
    return store;
  }
}

package org.ethereum.beacon.simulator;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.ethereum.beacon.Launcher;
import org.ethereum.beacon.bench.BenchSpecRegistry;
import org.ethereum.beacon.bench.BenchSpecRegistryImpl;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.transition.EpochTransitionSummary;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.util.BlsKeyPairReader;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.LoggerMDCExecutor;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.schedulers.TimeController;
import org.ethereum.beacon.schedulers.TimeControllerImpl;
import org.ethereum.beacon.bench.BenchSpecWrapper;
import org.ethereum.beacon.simulator.util.SimulateUtils;
import org.ethereum.beacon.util.stats.TimeCollector;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.wire.LocalWireHub;
import org.ethereum.beacon.wire.WireApi;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BenchmarkRunner implements Runnable {
  private static final Logger logger = LogManager.getLogger("simulator");
  private static final Logger wire = LogManager.getLogger("wire");

  private final int epochCount;
  private final int validatorCount;
  private final SpecConstants specConstants;
  private final BeaconChainSpec spec;
  private final SpecBuilder specBuilder;
  private final boolean blsEnabled;

  public BenchmarkRunner(
      int epochCount, int validatorCount, SpecBuilder specBuilder, boolean blsEnabled) {
    this.epochCount = epochCount;
    this.validatorCount = validatorCount;
    this.specBuilder = specBuilder;
    this.specConstants = specBuilder.buildSpecConstants();
    this.spec = specBuilder.buildSpec();
    this.blsEnabled = blsEnabled;
  }

  private void setupLogging() {
    try (InputStream inputStream = ClassLoader.class.getResourceAsStream("/log4j2.xml")) {
      ConfigurationSource source = new ConfigurationSource(inputStream);
      Configurator.initialize(null, source);
    } catch (Exception e) {
      throw new RuntimeException("Cannot read log4j default configuration", e);
    }

    LoggerContext context =
        (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
    Configuration config = context.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig("simulator");
    loggerConfig.setLevel(Level.INFO);
    context.updateLoggers();
  }

  private Pair<List<Deposit>, List<KeyPair>> getValidatorDeposits(BeaconChainSpec spec, int count) {
    List<Deposit> deposits = new ArrayList<>();
    List<BLS381.KeyPair> keyPairs = new ArrayList<>();
    Random rnd = new Random();
    BlsKeyPairReader keyPairReader = BlsKeyPairReader.createWithDefaultSource();

    for (int i = 0; i < count; i++) {
      BLS381.KeyPair keyPair = keyPairReader.next();
      keyPairs.add(keyPair);
      deposits.add(SimulateUtils.getDepositForKeyPair(rnd, keyPair, spec, false));
    }

    return Pair.with(deposits, keyPairs);
  }

  public void run() {
    Time genesisTime = Time.ZERO;

    setupLogging();
    Pair<List<Deposit>, List<KeyPair>> validatorDeposits =
        getValidatorDeposits(spec, validatorCount);

    List<Deposit> deposits = validatorDeposits.getValue0().stream()
        .filter(Objects::nonNull).collect(Collectors.toList());
    List<KeyPair> keyPairs = validatorDeposits.getValue1();

    MDCControlledSchedulers controlledSchedulers = new MDCControlledSchedulers();
    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);

    Eth1Data eth1Data = new Eth1Data(Hash32.ZERO, Hash32.ZERO);

    LocalWireHub localWireHub =
        new LocalWireHub(s -> wire.trace(s), controlledSchedulers.createNew("wire"));
    DepositContract.ChainStart chainStart =
        new DepositContract.ChainStart(genesisTime, eth1Data, deposits);
    DepositContract depositContract = new SimpleDepositContract(chainStart);

    logger.info("Creating validators...");
    TimeCollector proposeTimeCollector = new TimeCollector();

    ControlledSchedulers schedulers = controlledSchedulers.createNew("V0");
    WireApi wireApi = localWireHub.createNewPeer("0");

    List<BLS381Credentials> blsCreds;
    if (blsEnabled) {
      blsCreds =
          keyPairs.stream()
              .map(BLS381Credentials::createWithInsecureSigner)
              .collect(Collectors.toList());
    } else {
      blsCreds =
          keyPairs.stream()
              .map(BLS381Credentials::createWithDummySigner)
              .collect(Collectors.toList());
    }

    BenchSpecRegistry benchRegistry = BenchSpecRegistry.newInstance();
    Launcher instance =
        new Launcher(
            specBuilder.buildSpec(),
            depositContract,
            blsCreds,
            wireApi,
            new MemBeaconChainStorageFactory(),
            schedulers,
            proposeTimeCollector,
            benchRegistry);

    logger.info("All validators created");

    List<SlotNumber> slots = new ArrayList<>();
    List<Attestation> attestations = new ArrayList<>();
    List<BeaconBlock> blocks = new ArrayList<>();
    List<ObservableBeaconState> states = new ArrayList<>();

    Flux.from(instance.getSlotTicker().getTickerStream()).subscribe(slot -> {
      slots.add(slot);
      logger.debug("New slot: " + slot.toString(specConstants, genesisTime));
    });
    Flux.from(instance.getObservableStateProcessor().getObservableStateStream())
        .subscribe(os -> {
          states.add(os);
          logger.debug("New observable state: " + os.toString(spec));
        });
    Flux.from(instance.getValidatorService().getAttestationsStream())
        .publishOn(instance.getSchedulers().reactorEvents())
        .subscribe(att -> {
          attestations.add(att);
          logger.debug("New attestation received: " + att.toStringShort(specConstants));
        });
    Flux.from(instance.getBeaconChain().getBlockStatesStream())
        .subscribe(blockState -> {
          blocks.add(blockState.getBlock());
          logger.debug("Block imported: "
              + blockState.getBlock().toString(specConstants, genesisTime, spec::signed_root));
        });

    logger.info("Time starts running ...");
    controlledSchedulers.setCurrentTime(
        genesisTime.plus(specConstants.getSecondsPerSlot()).getMillis().getValue() - 9);

    // skip 1st epoch, add extra slot to trigger last epoch transition
    int slotsToRun = (epochCount + 1) * specConstants.getSlotsPerEpoch().getIntValue();
    for (int i = 0; i < slotsToRun; i++) {
      // start tracking slots and blocks since the beginning of the 2nd epoch
      if (i == specConstants.getSlotsPerEpoch().getIntValue()) {
        benchRegistry.get(BenchSpecRegistry.SLOT_BENCH).ifPresent(BenchSpecWrapper::startTracking);
        benchRegistry.get(BenchSpecRegistry.BLOCK_BENCH).ifPresent(BenchSpecWrapper::startTracking);
      }
      // start tracking epochs when first epoch transition has happened
      if (i == specConstants.getSlotsPerEpoch().getIntValue() + 1) {
        benchRegistry.get(BenchSpecRegistry.EPOCH_BENCH).ifPresent(BenchSpecWrapper::startTracking);
      }

      controlledSchedulers.addTime(
          Duration.ofMillis(specConstants.getSecondsPerSlot().getMillis().getValue()));

      if (slots.size() > 1) {
        logger.warn("More than 1 slot generated: " + slots);
      }
      if (slots.isEmpty()) {
        logger.error("No slots generated");
      }

      logger.info("Slot " + slots.get(0).toStringNumber(specConstants)
          + ", committee: " + spec
          .get_crosslink_committees_at_slot(states.get(0).getLatestSlotState(), slots.get(0))
          + ", blocks: " + blocks.size()
          + ", attestations: " + attestations.size());

      ObservableBeaconState latestState = states.get(states.size() - 1);
      if (latestState.getLatestSlotState().getSlot().increment().modulo(spec.getConstants().getSlotsPerEpoch())
          .equals(SlotNumber.ZERO)) {
        ObservableBeaconState preEpochState = latestState;
        EpochTransitionSummary summary = instance.getExtendedSlotTransition()
            .applyWithSummary(preEpochState.getLatestSlotState());
        logger.info("Epoch transition "
            + spec.get_current_epoch(preEpochState.getLatestSlotState()).toString(specConstants)
            + "=>"
            + spec.get_current_epoch(preEpochState.getLatestSlotState()).increment().toString(specConstants)
            + ": Justified/Finalized epochs: "
            + summary.getPreState().getCurrentJustifiedEpoch().toString(specConstants)
            + "/"
            + summary.getPreState().getFinalizedEpoch().toString(specConstants)
            + " => "
            + summary.getPostState().getCurrentJustifiedEpoch().toString(specConstants)
            + "/"
            + summary.getPostState().getFinalizedEpoch().toString(specConstants)
        );
        logger.info("  Validators rewarded:"
            + getValidators(" attestations: ", summary.getAttestationRewards())
            + getValidators(" boundary: ", summary.getBoundaryAttestationRewards())
            + getValidators(" head: ", summary.getBeaconHeadAttestationRewards())
            + getValidators(" include distance: ", summary.getInclusionDistanceRewards())
            + getValidators(" attest inclusion: ", summary.getAttestationInclusionRewards())
        );
        logger.info("  Validators penalized:"
            + getValidators(" attestations: ", summary.getAttestationPenalties())
            + getValidators(" boundary: ", summary.getBoundaryAttestationPenalties())
            + getValidators(" head: ", summary.getBeaconHeadAttestationPenalties())
            + getValidators(" penalized epoch: ", summary.getInitiatedExitPenalties())
            + getValidators(" no finality: ", summary.getNoFinalityPenalties())
        );
      }

      slots.clear();
      attestations.clear();
      blocks.clear();
      states.clear();
    }

    benchRegistry
        .get(BenchSpecRegistry.SLOT_BENCH)
        .ifPresent(b -> System.out.println("Slot processing:\n" + b.buildReport()));
    benchRegistry
        .get(BenchSpecRegistry.BLOCK_BENCH)
        .ifPresent(b -> System.out.println("Block processing:\n" + b.buildReport()));
    benchRegistry
        .get(BenchSpecRegistry.EPOCH_BENCH)
        .ifPresent(b -> System.out.println("Epoch processing:\n" + b.buildReport()));
  }

  private static String getValidators(String info, Map<ValidatorIndex, ?> records) {
    if (records.isEmpty()) return "";
    return info + " ["
        + records.entrySet().stream().map(e -> e.getKey().toString()).collect(Collectors.joining(","))
        + "]";
  }

  private static class SimpleDepositContract implements DepositContract {
    private final ChainStart chainStart;

    public SimpleDepositContract(ChainStart chainStart) {
      this.chainStart = chainStart;
    }

    @Override
    public Publisher<ChainStart> getChainStartMono() {
      return Mono.just(chainStart);
    }

    @Override
    public Publisher<Deposit> getDepositStream() {
      return Mono.empty();
    }

    @Override
    public List<DepositInfo> peekDeposits(
        int maxCount, Eth1Data fromDepositExclusive, Eth1Data tillDepositInclusive) {
      return Collections.emptyList();
    }

    @Override
    public boolean hasDepositRoot(Hash32 blockHash, Hash32 depositRoot) {
      return true;
    }

    @Override
    public Optional<Eth1Data> getLatestEth1Data() {
      return Optional.of(chainStart.getEth1Data());
    }

    @Override
    public void setDistanceFromHead(long distanceFromHead) {}
  }

  public static class MDCControlledSchedulers {
    private DateFormat localTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    private TimeController timeController = new TimeControllerImpl();

    public ControlledSchedulers createNew(String validatorId) {
      return createNew(validatorId, 0);
    }

    public ControlledSchedulers createNew(String validatorId, long timeShift) {
      ControlledSchedulers[] newSched = new ControlledSchedulers[1];
      LoggerMDCExecutor mdcExecutor = new LoggerMDCExecutor()
          .add("validatorTime", () -> localTimeFormat.format(new Date(newSched[0].getCurrentTime())))
          .add("validatorIndex", () -> "" + validatorId);
      newSched[0] = Schedulers.createControlled(() -> mdcExecutor);
      newSched[0].getTimeController().setParent(timeController);
      newSched[0].getTimeController().setTimeShift(timeShift);

      return newSched[0];
    }

    public void setCurrentTime(long time) {
      timeController.setTime(time);
    }

    void addTime(Duration duration) {
      addTime(duration.toMillis());
    }

    void addTime(long millis) {
      setCurrentTime(timeController.getTime() + millis);
    }

    public long getCurrentTime() {
      return timeController.getTime();
    }
  }
}

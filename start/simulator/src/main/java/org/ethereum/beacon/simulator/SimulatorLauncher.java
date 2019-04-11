package org.ethereum.beacon.simulator;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.plan.SimulationPlan;
import org.ethereum.beacon.emulator.config.simulator.PeersConfig;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.LoggerMDCExecutor;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.schedulers.TimeController;
import org.ethereum.beacon.schedulers.TimeControllerImpl;
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
import tech.pegasys.artemis.util.bytes.Bytes32;

public class SimulatorLauncher implements Runnable {
  private static final Logger logger = LogManager.getLogger("simulator");
  private static final Logger wire = LogManager.getLogger("wire");

  private final MainConfig config;
  private final SimulationPlan simulationPlan;
  private final List<PeersConfig> validators;
  private final List<PeersConfig> observers;
  private final SpecConstants specConstants;
  private final BeaconChainSpec spec;
  private final Level logLevel;
  private final SpecBuilder specBuilder;

  /**
   * Creates Simulator launcher with following settings
   *
   * @param config configuration and run plan.
   * @param specBuilder chain specification builder.
   * @param validators validator peers configuration.
   * @param observers observer peers configuration.
   * @param logLevel Log level, Apache log4j type.
   */
  public SimulatorLauncher(
      MainConfig config,
      SpecBuilder specBuilder,
      List<PeersConfig> validators,
      List<PeersConfig> observers,
      Level logLevel) {
    this.config = config;
    this.simulationPlan = (SimulationPlan) config.getPlan();
    this.specBuilder = specBuilder;
    this.specConstants = specBuilder.buildSpecConstants();
    this.spec = specBuilder.buildSpec();
    this.validators = validators;
    this.observers = observers;
    this.logLevel = logLevel;
  }

  private void setupLogging() {
    try (InputStream inputStream = ClassLoader.class.getResourceAsStream("/log4j2.xml")) {
      ConfigurationSource source = new ConfigurationSource(inputStream);
      Configurator.initialize(null, source);
    } catch (Exception e) {
      throw new RuntimeException("Cannot read log4j default configuration", e);
    }

    // set logLevel
    if (logLevel != null) {
      LoggerContext context =
          (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
      Configuration config = context.getConfiguration();
      LoggerConfig loggerConfig = config.getLoggerConfig("simulator");
      loggerConfig.setLevel(logLevel);
      context.updateLoggers();
    }
  }

  private Pair<List<Deposit>, List<BLS381.KeyPair>> getValidatorDeposits(Random rnd) {
    Pair<List<Deposit>, List<BLS381.KeyPair>> deposits =
        SimulateUtils.getAnyDeposits(rnd, spec, validators.size(),
            config.getChainSpec().getSpecHelpersOptions().isBlsVerifyProofOfPossession());
    for (int i = 0; i < validators.size(); i++) {
      if (validators.get(i).getBlsPrivateKey() != null) {
        KeyPair keyPair = KeyPair.create(
            PrivateKey.create(Bytes32.fromHexString(validators.get(i).getBlsPrivateKey())));
        deposits.getValue0().set(i, SimulateUtils.getDepositForKeyPair(rnd, keyPair, spec,
            config.getChainSpec().getSpecHelpersOptions().isBlsVerifyProofOfPossession()));
        deposits.getValue1().set(i, keyPair);
      }
    }

    return deposits;
  }

  public void run() {
    logger.info("Simulation parameters:\n{}", simulationPlan);
    if (config.getChainSpec().isDefined())
      logger.info("Overridden beacon chain parameters:\n{}", config.getChainSpec());

    Random rnd = new Random(simulationPlan.getSeed());
    setupLogging();
    Pair<List<Deposit>, List<BLS381.KeyPair>> validatorDeposits = getValidatorDeposits(rnd);

    List<Deposit> deposits = validatorDeposits.getValue0().stream()
        .filter(Objects::nonNull).collect(Collectors.toList());
    List<BLS381.KeyPair> keyPairs = validatorDeposits.getValue1();

    Time genesisTime = Time.of(simulationPlan.getGenesisTime());

    MDCControlledSchedulers controlledSchedulers = new MDCControlledSchedulers();
    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);

    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));

    LocalWireHub localWireHub =
        new LocalWireHub(s -> wire.trace(s), controlledSchedulers.createNew("wire"));
    DepositContract.ChainStart chainStart =
        new DepositContract.ChainStart(genesisTime, eth1Data, deposits);
    DepositContract depositContract = new SimpleDepositContract(chainStart);

    List<Launcher> peers = new ArrayList<>();

    logger.info("Creating validators...");
    TimeCollector proposeTimeCollector = new TimeCollector();
    for (int i = 0; i < validators.size(); i++) {
      ControlledSchedulers schedulers =
          controlledSchedulers.createNew("V" + i, validators.get(i).getSystemTimeShift());
      WireApi wireApi =
          localWireHub.createNewPeer(
              "" + i,
              validators.get(i).getWireInboundDelay(),
              validators.get(i).getWireOutboundDelay());

      BLS381Credentials bls;
      if (keyPairs.get(i) == null) {
        bls = null;
      } else {
        bls = config.getChainSpec().getSpecHelpersOptions().isBlsSign() ?
            BLS381Credentials.createWithInsecureSigner(keyPairs.get(i)) :
            BLS381Credentials.createWithDummySigner(keyPairs.get(i));
      }

      Launcher launcher =
          new Launcher(
              specBuilder.buildSpec(),
              depositContract,
              Collections.singletonList(bls),
              wireApi,
              new MemBeaconChainStorageFactory(),
              schedulers,
              proposeTimeCollector);

      peers.add(launcher);

      if ((i + 1) % 100 == 0)
        logger.info("{} validators created", (i + 1));
    }
    logger.info("All validators created");

    logger.info("Creating observer peers...");
    for (int i = 0; i < observers.size(); i++) {
      PeersConfig config = observers.get(i);
      String name = "O" + i;
      Launcher launcher =
          new Launcher(
              specBuilder.buildSpec(),
              depositContract,
              null,
              localWireHub.createNewPeer(
                  name, config.getWireInboundDelay(), config.getWireOutboundDelay()),
              new MemBeaconChainStorageFactory(),
              controlledSchedulers.createNew(name, config.getSystemTimeShift()));
      peers.add(launcher);
    }

    Map<Integer, ObservableBeaconState> latestStates = new HashMap<>();
    for (int i = 0; i < peers.size(); i++) {
      Launcher launcher = peers.get(i);

      int finalI = i;
      Flux.from(launcher.getSlotTicker().getTickerStream()).subscribe(slot ->
          logger.trace("New slot: " + slot.toString(this.specConstants, genesisTime)));
      Flux.from(launcher.getObservableStateProcessor().getObservableStateStream())
          .subscribe(os -> {
            latestStates.put(finalI, os);
            logger.trace("New observable state: " + os.toString(spec));
          });
      Flux.from(launcher.getBeaconChain().getBlockStatesStream())
          .subscribe(blockState -> logger.trace("Block imported: "
              + blockState.getBlock().toString(this.specConstants, genesisTime, spec::signed_root)));
      if (launcher.getValidatorService() != null) {
        Flux.from(launcher.getValidatorService().getProposedBlocksStream())
            .subscribe(block -> logger.debug("New block created: "
                + block.toString(this.specConstants, genesisTime, spec::signed_root)));
        Flux.from(launcher.getValidatorService().getAttestationsStream())
            .subscribe(attest -> logger.debug("New attestation created: "
                + attest.toString(this.specConstants, genesisTime)));
      }
    }

    // system observer
    ControlledSchedulers schedulers = controlledSchedulers.createNew("X");
    WireApi wireApi = localWireHub.createNewPeer("X");

    Launcher observer =
        new Launcher(
            spec,
            depositContract,
            null,
            wireApi,
            new MemBeaconChainStorageFactory(),
            schedulers);

    peers.add(observer);

    List<SlotNumber> slots = new ArrayList<>();
    List<Attestation> attestations = new ArrayList<>();
    List<BeaconBlock> blocks = new ArrayList<>();
    List<ObservableBeaconState> states = new ArrayList<>();

    Flux.from(observer.getSlotTicker().getTickerStream()).subscribe(slot -> {
      slots.add(slot);
      logger.debug("New slot: " + slot.toString(specConstants, genesisTime));
    });
    Flux.from(observer.getObservableStateProcessor().getObservableStateStream())
        .subscribe(os -> {
          latestStates.put(peers.size(), os);
          states.add(os);
          logger.debug("New observable state: " + os.toString(spec));
        });
    Flux.from(observer.getWireApi().inboundAttestationsStream())
        .publishOn(observer.getSchedulers().reactorEvents())
        .subscribe(att -> {
          attestations.add(att);
          logger.debug("New attestation received: " + att.toStringShort(specConstants));
        });
    Flux.from(observer.getBeaconChain().getBlockStatesStream())
        .subscribe(blockState -> {
          blocks.add(blockState.getBlock());
          logger.debug("Block imported: "
              + blockState.getBlock().toString(specConstants, genesisTime, spec::signed_root));
        });

    logger.info("Time starts running ...");
    controlledSchedulers.setCurrentTime(
        genesisTime.plus(specConstants.getSecondsPerSlot()).getMillis().getValue() - 9);
    while (true) {
      controlledSchedulers.addTime(
          Duration.ofMillis(specConstants.getSecondsPerSlot().getMillis().getValue()));

      if (slots.size() > 1) {
        logger.warn("More than 1 slot generated: " + slots);
      }
      if (slots.isEmpty()) {
        logger.error("No slots generated");
      }

      Map<Hash32, List<ObservableBeaconState>> grouping = latestStates.values().stream()
          .collect(Collectors.groupingBy(s -> spec.hash_tree_root(s.getLatestSlotState())));

      String statesInfo;
      if (grouping.size() == 1) {
        statesInfo = "all peers on the state " + grouping.keySet().iterator().next().toStringShort();
      } else {
        statesInfo = "peers states differ:  " + grouping.entrySet().stream()
            .map(e -> e.getKey().toStringShort() + ": " + e.getValue().size() + " peers")
            .collect(Collectors.joining(", "));

      }

      logger.info("Slot " + slots.get(0).toStringNumber(specConstants)
          + ", committee: " + spec
          .get_crosslink_committees_at_slot(states.get(0).getLatestSlotState(), slots.get(0))
          + ", blocks: " + blocks.size()
          + ", attestations: " + attestations.size()
          + ", " + statesInfo);

      ObservableBeaconState latestState = states.get(states.size() - 1);
      if (latestState.getLatestSlotState().getSlot().increment().modulo(spec.getConstants().getSlotsPerEpoch())
          .equals(SlotNumber.ZERO)) {
        ObservableBeaconState preEpochState = latestState;
        EpochTransitionSummary summary = observer.getExtendedSlotTransition()
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

  public static class Builder {
    private MainConfig config;
    private Level logLevel = Level.INFO;

    public Builder() {}

    public SimulatorLauncher build() {
      assert config != null;
      SimulationPlan simulationPlan = (SimulationPlan) config.getPlan();

      ConfigBuilder<SpecData> specConfigBuilder =
          new ConfigBuilder<>(SpecData.class).addYamlConfigFromResources("/config/spec-constants.yml");
      if (config.getChainSpec().isDefined()) {
        specConfigBuilder.addConfig(config.getChainSpec());
      }

      SpecData spec = specConfigBuilder.build();

      List<PeersConfig> peers = new ArrayList<>();
      for (PeersConfig peer : simulationPlan.getPeers()) {
        for (int i = 0; i < peer.getCount(); i++) {
          peers.add(peer);
        }
      }

      SpecBuilder specBuilder = new SpecBuilder().withSpec(spec);

      return new SimulatorLauncher(
          config,
          specBuilder,
          peers.stream().filter(PeersConfig::isValidator).collect(Collectors.toList()),
          peers.stream().filter(config -> !config.isValidator()).collect(Collectors.toList()),
          logLevel);
    }

    public Builder withConfigFromFile(File file) {
      this.config = new ConfigBuilder<>(MainConfig.class).addYamlConfig(file).build();
      return this;
    }

    public Builder withConfigFromResource(String resourceName) {
      this.config =
          new ConfigBuilder<>(MainConfig.class)
              .addYamlConfigFromResources(resourceName)
              .build();
      return this;
    }

    public Builder withLogLevel(Level logLevel) {
      this.logLevel = logLevel;
      return this;
    }
  }
}

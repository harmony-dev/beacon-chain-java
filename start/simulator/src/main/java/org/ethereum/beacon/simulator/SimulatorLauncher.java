package org.ethereum.beacon.simulator;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.consensus.transition.EpochTransitionSummary;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.plan.SimulationPlan;
import org.ethereum.beacon.emulator.config.simulator.PeersConfig;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.start.common.Launcher;
import org.ethereum.beacon.start.common.util.MDCControlledSchedulers;
import org.ethereum.beacon.start.common.util.SimpleDepositContract;
import org.ethereum.beacon.start.common.util.SimulateUtils;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.wire.LocalWireHub;
import org.ethereum.beacon.wire.WireApiSub;
import org.javatuples.Pair;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.uint.UInt64;

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

  private Random rnd;
  private Time genesisTime;
  private MDCControlledSchedulers controlledSchedulers;
  private LocalWireHub localWireHub;
  private List<BLS381.KeyPair> keyPairs;
  private Eth1Data eth1Data;
  private DepositContract depositContract;

  private List<Launcher> peers;

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

    init();
  }

  private void setupLogging() {
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

  public void init() {
    rnd = new Random(simulationPlan.getSeed());
    setupLogging();
    Pair<List<Deposit>, List<BLS381.KeyPair>> validatorDeposits = getValidatorDeposits(rnd);

    List<Deposit> deposits = validatorDeposits.getValue0().stream()
        .filter(Objects::nonNull).collect(Collectors.toList());
    keyPairs = validatorDeposits.getValue1();

    genesisTime = Time.of(simulationPlan.getGenesisTime());

    controlledSchedulers = new MDCControlledSchedulers();
    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);

    eth1Data = new Eth1Data(Hash32.random(rnd), UInt64.valueOf(deposits.size()), Hash32.random(rnd));

    localWireHub = new LocalWireHub(s -> wire.trace(s), controlledSchedulers.createNew("wire"));
    ChainStart chainStart =
        new ChainStart(genesisTime, eth1Data, deposits);
    depositContract = new SimpleDepositContract(chainStart);
  }

  public Launcher createPeer(String name) {
    return createPeer(new PeersConfig(), null, name);
  }
  public Launcher createPeer(PeersConfig config, BLS381Credentials bls, String name) {
    WireApiSub wireApi =
        localWireHub.createNewPeer(
            name,
            config.getWireInboundDelay(),
            config.getWireOutboundDelay());
    return createPeer(config, bls, wireApi, name);
  }

  public Launcher createPeer(PeersConfig config, BLS381Credentials bls, WireApiSub wireApi, String name) {
    ControlledSchedulers schedulers =
        controlledSchedulers.createNew(name, config.getSystemTimeShift());

    BeaconChainSpec spec = specBuilder.buildSpec();
    return new Launcher(
            spec,
            depositContract,
            bls == null ? null : Collections.singletonList(bls),
            wireApi,
            new MemBeaconChainStorageFactory(spec.getObjectHasher()),
            schedulers);
  }

  public void run() {
    run(Integer.MAX_VALUE);
  }

  public void run(int slotsCount) {
    logger.info("Simulation parameters:\n{}", simulationPlan);
    if (config.getChainSpec().isDefined())
      logger.info("Overridden beacon chain parameters:\n{}", config.getChainSpec());

    peers = new ArrayList<>();

    logger.info("Creating validators...");
    for (int i = 0; i < validators.size(); i++) {

      BLS381Credentials bls;
      if (keyPairs.get(i) == null) {
        bls = null;
      } else {
        bls = config.getChainSpec().getSpecHelpersOptions().isBlsSign() ?
            BLS381Credentials.createWithInsecureSigner(keyPairs.get(i)) :
            BLS381Credentials.createWithDummySigner(keyPairs.get(i));
      }

      peers.add(createPeer(validators.get(i), bls, "V" + i));

      if ((i + 1) % 100 == 0)
        logger.info("{} validators created", (i + 1));
    }
    logger.info("All validators created");

    logger.info("Creating observer peers...");
    for (int i = 0; i < observers.size(); i++) {
      peers.add(createPeer(observers.get(i), null, "O" + i));
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
              + blockState.getBlock().toString(this.specConstants, genesisTime, spec::signing_root)));
      if (launcher.getValidatorService() != null) {
        Flux.from(launcher.getValidatorService().getProposedBlocksStream())
            .subscribe(block -> logger.debug("New block created: "
                + block.toString(this.specConstants, genesisTime, spec::signing_root)));
        Flux.from(launcher.getValidatorService().getAttestationsStream())
            .subscribe(attest -> logger.debug("New attestation created: "
                + attest.toString(this.specConstants, genesisTime)));
      }
    }

    // system observer
    Launcher observer = createPeer("X");
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
        .publishOn(observer.getSchedulers().events().toReactor())
        .subscribe(att -> {
          attestations.add(att);
          logger.debug("New attestation received: " + att.toStringShort(specConstants));
        });
    Flux.from(observer.getBeaconChain().getBlockStatesStream())
        .subscribe(blockState -> {
          blocks.add(blockState.getBlock());
          logger.debug("Block imported: "
              + blockState.getBlock().toString(specConstants, genesisTime, spec::signing_root));
        });

    logger.info("Time starts running ...");
    controlledSchedulers.setCurrentTime(
        genesisTime.plus(specConstants.getSecondsPerSlot()).getMillis().getValue() - 9);
    for (int i = 0; i < slotsCount; i++) {
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
            .getEpochTransitionSummary(preEpochState.getLatestSlotState());
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
            + getValidators(" attestations: ", summary.getAttestationDeltas()[0])
            + getValidators(" crosslinks: ", summary.getCrosslinkDeltas()[0])
        );
        logger.info("  Validators penalized:"
            + getValidators(" attestations: ", summary.getAttestationDeltas()[1])
            + getValidators(" crosslinks: ", summary.getCrosslinkDeltas()[1])
        );
      }

      slots.clear();
      attestations.clear();
      blocks.clear();
      states.clear();
    }
  }

  public List<Launcher> getPeers() {
    return peers;
  }

  public BeaconChainSpec getSpec() {
    return spec;
  }

  public Random getRnd() {
    return rnd;
  }

  public Time getGenesisTime() {
    return genesisTime;
  }

  public MDCControlledSchedulers getControlledSchedulers() {
    return controlledSchedulers;
  }

  public LocalWireHub getLocalWireHub() {
    return localWireHub;
  }

  public DepositContract getDepositContract() {
    return depositContract;
  }

  private static String getValidators(String info, Gwei[] balances) {
    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < balances.length; i++) {
      if (balances[i].greater(Gwei.ZERO)) {
        indices.add(i);
      }
    }
    return indices.isEmpty() ? "" : info + " ["
        + indices.stream().map(String::valueOf).collect(Collectors.joining(","))
        + "]";
  }

  public static class Builder {
    private MainConfig config;
    private Level logLevel = Level.INFO;

    public Builder() {}

    public SimulatorLauncher build() {
      assert config != null;
      SimulationPlan simulationPlan = (SimulationPlan) config.getPlan();

      ConfigBuilder<SpecData> specConfigBuilder =
          new ConfigBuilder<>(SpecData.class).addYamlConfigFromResources(
              "/config/spec-constants.yml");
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

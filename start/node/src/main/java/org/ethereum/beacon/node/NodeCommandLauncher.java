package org.ethereum.beacon.node;

import java.io.File;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.plan.SimulationPlan;
import org.ethereum.beacon.emulator.config.simulator.PeersConfig;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.start.common.NodeLauncher;
import org.ethereum.beacon.start.common.util.MDCControlledSchedulers;
import org.ethereum.beacon.start.common.util.SimpleDepositContract;
import org.ethereum.beacon.start.common.util.SimulateUtils;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.wire.net.ConnectionManager;
import org.ethereum.beacon.wire.net.netty.NettyServer;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class NodeCommandLauncher implements Runnable {
  private static final Logger logger = LogManager.getLogger("node");

  private final MainConfig config;
  private final SpecConstants specConstants;
  private final BeaconChainSpec spec;
  private final Level logLevel;
  private final SpecBuilder specBuilder;

  private Random rnd;
  private Time genesisTime;
  private MDCControlledSchedulers controlledSchedulers;
  private List<KeyPair> keyPairs;
  private Eth1Data eth1Data;
  private DepositContract depositContract;

  /**
   * Creates launcher with following settings
   *
   * @param config configuration and run plan.
   * @param specBuilder chain specification builder.
   * @param logLevel Log level, Apache log4j type.
   */
  public NodeCommandLauncher(
      MainConfig config,
      SpecBuilder specBuilder,
      Level logLevel) {
    this.config = config;
    this.specBuilder = specBuilder;
    this.specConstants = specBuilder.buildSpecConstants();
    this.spec = specBuilder.buildSpec();
    this.logLevel = logLevel;

    init();
  }

  private void setupLogging() {
    // set logLevel
    if (logLevel != null) {
      LoggerContext context =
          (LoggerContext) LogManager.getContext(false);
      Configuration config = context.getConfiguration();
      LoggerConfig loggerConfig = config.getLoggerConfig("node");
      loggerConfig.setLevel(logLevel);
      context.updateLoggers();
    }
  }

  public void init() {
    setupLogging();

//    List<Deposit> deposits = validatorDeposits.getValue0().stream()
//        .filter(Objects::nonNull).collect(Collectors.toList());
//    keyPairs = validatorDeposits.getValue1();
//
//    genesisTime = Time.of(simulationPlan.getGenesisTime());
//
//    controlledSchedulers = new MDCControlledSchedulers();
//    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);
//
//    eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));
//
//    DepositContract.ChainStart chainStart =
//        new DepositContract.ChainStart(genesisTime, eth1Data, deposits);
//    depositContract = new SimpleDepositContract(chainStart);
  }

  public void run() {
    if (config.getChainSpec().isDefined())
      logger.info("Overridden beacon chain parameters:\n{}", config.getChainSpec());

    Random rnd = new Random();

    ConfigBuilder<SpecData> specConfigBuilder =
        new ConfigBuilder<>(SpecData.class)
            .addYamlConfigFromResources("/config/spec-constants.yml")
            .addYamlConfigFromResources("/test-spec-config.yml");
    SpecData specData = specConfigBuilder.build();
    SpecBuilder specBuilder = new SpecBuilder().withSpec(specData);
    BeaconChainSpec spec = specBuilder.buildSpec();

    int depositCount = 16;
    Pair<List<Deposit>, List<KeyPair>> depositPairs =
        SimulateUtils.getAnyDeposits(rnd, spec, depositCount, false);

    Time genesisTime = Time.of(60000);

    MDCControlledSchedulers controlledSchedulers = new MDCControlledSchedulers();
    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);

    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), UInt64.valueOf(depositCount), Hash32.random(rnd));

    DepositContract.ChainStart chainStart =
        new DepositContract.ChainStart(genesisTime, eth1Data, depositPairs.getValue0());
    SimpleDepositContract depositContract = new SimpleDepositContract(chainStart);

    // master node with all validators
    {
      ControlledSchedulers schedulers = controlledSchedulers.createNew("master");
      NettyServer nettyServer = new NettyServer(40001);
      nettyServer.start();
      ConnectionManager<SocketAddress> connectionManager = new ConnectionManager<>(
          nettyServer, null, schedulers.reactorEvents());
      NodeLauncher masterNode = new NodeLauncher(
          specBuilder.buildSpec(),
          depositContract,
          depositPairs
              .getValue1()
              .stream()
              .map(BLS381Credentials::createWithDummySigner)
              .collect(Collectors.toList()),
          connectionManager,
          new MemBeaconChainStorageFactory(spec.getObjectHasher()),
          schedulers,
          false);
    }

  }

  public static class Builder {
    private MainConfig config;
    private Level logLevel = Level.INFO;

    public Builder() {}

    public NodeCommandLauncher build() {
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

      return new NodeCommandLauncher(
          config,
          specBuilder,
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

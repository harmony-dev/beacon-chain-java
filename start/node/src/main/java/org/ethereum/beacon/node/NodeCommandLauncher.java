package org.ethereum.beacon.node;

import io.netty.channel.ChannelFutureListener;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Random;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.network.NettyNetwork;
import org.ethereum.beacon.emulator.config.main.network.Network;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.start.common.NodeLauncher;
import org.ethereum.beacon.start.common.util.MDCControlledSchedulers;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.wire.net.ConnectionManager;
import org.ethereum.beacon.wire.net.netty.NettyClient;
import org.ethereum.beacon.wire.net.netty.NettyServer;

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
  }

  public void run() {
    if (config.getChainSpec().isDefined())
      logger.info("Overridden beacon chain parameters:\n{}", config.getChainSpec());

    Random rnd = new Random();
    Schedulers schedulers = Schedulers.createDefault();

    depositContract = ConfigUtils.createDepositContract(
        config.getConfig().getValidator().getContract(),
        spec,
        config.getChainSpec().getSpecHelpersOptions().isBlsVerifyProofOfPossession());

    List<BLS381Credentials> credentials = ConfigUtils.createCredentials(
        config.getConfig().getValidator().getSigner(),
        config.getChainSpec().getSpecHelpersOptions().isBlsSign());

    ConnectionManager<?> connectionManager;
    if (config.getConfig().getNetworks().size() != 1) {
      throw new IllegalArgumentException("1 network should be specified in config");
    }
    Network networkCfg = config.getConfig().getNetworks().get(0);
    if (networkCfg instanceof NettyNetwork) {
      NettyNetwork nettyConfig = (NettyNetwork) networkCfg;
      NettyServer nettyServer = null;
      if (nettyConfig.getListenPort() != null) {
        nettyServer = new NettyServer(nettyConfig.getListenPort());
        nettyServer.start().addListener((ChannelFutureListener) channelFuture -> {
          try {
            channelFuture.get();
            logger.info("Listening for inbound connections on port " + nettyConfig.getListenPort());
          } catch (Exception e) {
            logger.error("Unable to open inbound port " + nettyConfig.getListenPort(), e);
          }
        });
      }
      NettyClient nettyClient = new NettyClient();
      ConnectionManager<SocketAddress> tcpConnectionManager =
          new ConnectionManager<>(nettyServer, nettyClient, schedulers.reactorEvents());
      connectionManager = tcpConnectionManager;
      for (String addr : nettyConfig.getActivePeers()) {
        URI uri = URI.create(addr);
        tcpConnectionManager.addActivePeer(InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort()));
      }
    } else {
      throw new IllegalArgumentException(
          "This type of network is not supported yet: " + networkCfg.getClass());
    }

    NodeLauncher node = new NodeLauncher(
        specBuilder.buildSpec(),
        this.depositContract,
        credentials,
        connectionManager,
        new MemBeaconChainStorageFactory(spec.getObjectHasher()),
        schedulers,
        false);
  }

  public static class Builder {
    private MainConfig config;
    private Level logLevel = Level.INFO;

    public Builder() {}

    public NodeCommandLauncher build() {
      assert config != null;

      ConfigBuilder<SpecData> specConfigBuilder =
          new ConfigBuilder<>(SpecData.class).addYamlConfigFromResources("/config/spec-constants.yml");
      if (config.getChainSpec().isDefined()) {
        specConfigBuilder.addConfig(config.getChainSpec());
      }

      SpecData spec = specConfigBuilder.build();

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
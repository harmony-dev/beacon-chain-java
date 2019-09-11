package org.ethereum.beacon.node;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.impl.SSZBeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.chain.storage.util.StorageUtils;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.ConfigException;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsDataMerged;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.main.Debug;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.Signer.Insecure;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys.Private;
import org.ethereum.beacon.emulator.config.main.conract.EmulatorContract;
import org.ethereum.beacon.emulator.config.main.network.Libp2pNetwork;
import org.ethereum.beacon.emulator.config.main.network.NettyNetwork;
import org.ethereum.beacon.emulator.config.main.network.Network;
import org.ethereum.beacon.node.metrics.Metrics;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.DefaultSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.start.common.DatabaseManager;
import org.ethereum.beacon.start.common.NodeLauncher;
import org.ethereum.beacon.start.common.util.MDCControlledSchedulers;
import org.ethereum.beacon.start.common.util.SimpleDepositContract;
import org.ethereum.beacon.util.Objects;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.wire.impl.libp2p.Libp2pLauncher;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class NodeCommandLauncher implements Runnable {
  private static final Logger logger = LogManager.getLogger("node");

  private final static long DB_BUFFER_SIZE = 64L << 20; // 64Mb

  private final MainConfig config;
  private final SpecConstants specConstants;
  private final BeaconChainSpec spec;
  private final Level logLevel;
  private final SpecBuilder specBuilder;
  private final Node cliOptions;

  private Random rnd;
  private Time genesisTime;
  private MDCControlledSchedulers controlledSchedulers;
  private List<KeyPair> keyPairs;
  private Eth1Data eth1Data;

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
      Node cliOptions,
      Level logLevel) {
    this.config = config;
    this.specBuilder = specBuilder;
    this.specConstants = specBuilder.buildSpecConstants();
    this.spec = specBuilder.buildSpec();
    this.logLevel = logLevel;
    this.cliOptions = cliOptions;

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
    String nodeName = config.getConfig().getName();
    if (nodeName != null) {
      ThreadContext.put("validatorIndex", nodeName);
    }

    if (config.getChainSpec().isDefined())
      logger.info("Overridden beacon chain parameters:\n{}", config.getChainSpec());

    Schedulers schedulers =
        new DefaultSchedulers() {
          @Override
          protected ThreadFactory createThreadFactory(String namePattern) {
            ThreadFactory factory =
                createThreadFactoryBuilder((nodeName == null ? "" : nodeName + "-") + namePattern).build();
            if (nodeName == null) {
              return factory;
            } else {
              return r ->
                  factory.newThread(
                      () -> {
                        ThreadContext.put("validatorIndex", nodeName);
                        r.run();
                      });
            }
          }
        };

    String initialStateFile = cliOptions.getInitialStateFile();
    ChainStart chainStart;
    BeaconStateEx initialState;
    SerializerFactory serializerFactory = SerializerFactory.createSSZ(specConstants);
    if (initialStateFile == null) {
      chainStart = ConfigUtils.createChainStart(
          config.getConfig().getValidator().getContract(),
          spec,
          config.getChainSpec().getSpecHelpersOptions().isBlsVerifyProofOfPossession());
      initialState = new InitialStateTransition(chainStart, spec).apply(spec.get_empty_block());
    } else {
      byte[] fileData;
      try {
        fileData = Files.readAllBytes(Paths.get(initialStateFile));
      } catch (IOException e) {
        throw new IllegalArgumentException("Cannot read state file " + initialStateFile, e);
      }
      initialState =
          new BeaconStateExImpl(
              serializerFactory.getDeserializer(BeaconStateImpl.class).apply(BytesValue.wrap(fileData)),
              TransitionType.INITIAL);
      chainStart =
          new ChainStart(
              initialState.getGenesisTime(), initialState.getEth1Data(), Collections.emptyList());
    }
    DepositContract depositContract = new SimpleDepositContract(chainStart);

    List<BLS381Credentials> credentials = ConfigUtils.createCredentials(
        config.getConfig().getValidator().getSigner(),
        config.getChainSpec().getSpecHelpersOptions().isBlsSign());

    if (config.getConfig().getNetworks().size() != 1) {
      throw new IllegalArgumentException("1 network should be specified in config");
    }
    Network networkCfg = config.getConfig().getNetworks().get(0);
    Libp2pLauncher libp2pLauncher = new Libp2pLauncher();

    if (networkCfg instanceof NettyNetwork) {
      throw new UnsupportedOperationException("Netty network is not supported anymore");
      /*
      NettyNetwork nettyConfig = (NettyNetwork) networkCfg;
      NettyServer nettyServer = null;
      if (nettyConfig.getListenPort() != null) {
        Scheduler serverScheduler = schedulers.newParallelDaemon("netty-server-%d", 16);
        nettyServer = new NettyServer(nettyConfig.getListenPort(), serverScheduler::executeR);
        nettyServer.start().addListener((ChannelFutureListener) channelFuture -> {
          try {
            channelFuture.get();
          } catch (Exception e) {
            logger.error("Unable to open inbound port " + nettyConfig.getListenPort(), e);
          }
        });
      }
      Scheduler clientScheduler = schedulers.newParallelDaemon("netty-client-%d", 2);
      NettyClient nettyClient = new NettyClient(clientScheduler::executeR);
      ConnectionManager<SocketAddress> tcpConnectionManager =
          new ConnectionManager<>(nettyServer, nettyClient, schedulers.events());
      connectionManager = tcpConnectionManager;
      for (String addr : nettyConfig.getActivePeers()) {
        URI uri = URI.create(addr);
        tcpConnectionManager
            .addActivePeer(InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort()));
      }
      */
    } else if (networkCfg instanceof Libp2pNetwork) {
      Libp2pNetwork cfg = (Libp2pNetwork) networkCfg;

      if (cfg.getListenPort() != null) {
        libp2pLauncher.setListenPort(cfg.getListenPort());
      }
      for (String peer : cfg.getActivePeers()) {
        libp2pLauncher.addActivePeer(peer);
      }
      if (cfg.getPrivateKey() != null) {
        libp2pLauncher.setPrivKey(BytesValue.fromHexString(cfg.getPrivateKey()));
      }
      Debug debug = config.getConfig().getDebug();
      if (debug != null) {
        libp2pLauncher.setLogWireCipher(debug.isLogWireCipher());
        libp2pLauncher.setLogWirePlain(debug.isLogWirePlain());
        libp2pLauncher.setLogMuxFrames(debug.isLogMuxFrames());
        libp2pLauncher.setLogEthPubsub(debug.isLogEthPubsub());
        libp2pLauncher.setLogEthRpc(debug.isLogEthRpc());
      }
    } else {
      throw new IllegalArgumentException(
          "This type of network is not supported yet: " + networkCfg.getClass());
    }

    String metricsEndpoint = config.getConfig().getMetricsEndpoint();
    String metricsHost;
    int metricsPort;
    if (metricsEndpoint == null) {
      metricsHost = "0.0.0.0";
      metricsPort = 8008;
    } else {
      String[] parts = metricsEndpoint.split(":");
      if (parts.length != 2) {
        throw new IllegalArgumentException(
            "Wrong metrics endpoint format: \"" + metricsEndpoint + "\"");
      }
      metricsHost = parts[0];
      metricsPort = Integer.parseInt(parts[1]);
    }
    Metrics.startMetricsServer(metricsHost, metricsPort);

    SSZBeaconChainStorageFactory storageFactory =
        new SSZBeaconChainStorageFactory(
            spec.getObjectHasher(), serializerFactory);

    String dbPrefix = config.getConfig().getDb();
    String startMode;

    if (cliOptions.getStartMode() == null) {
      if (initialStateFile != null) {
        startMode = "initial";
      } else {
        startMode = "auto";
      }
    } else {
      startMode = cliOptions.getStartMode();
    }
    boolean forceDBClean = cliOptions.isForceDBClean();

    DatabaseManager dbFactory;
    if (dbPrefix == null) {
      dbFactory = DatabaseManager.createInMemoryDBFactory();
    } else {
      dbFactory = DatabaseManager.createRocksDBFactory(dbPrefix, DB_BUFFER_SIZE);
    }

    Time genesisTime = initialState.getGenesisTime();
    Hash32 depositRoot = initialState.getEth1Data().getDepositRoot();

    Database db = dbFactory.getOrCreateDatabase(genesisTime, depositRoot);
    BeaconChainStorage beaconChainStorage = storageFactory.create(db);

    boolean emptyStorage = beaconChainStorage.getTupleStorage().isEmpty();
    boolean doInitialize;
    switch (startMode) {
      case "storage":
        if (emptyStorage) {
          throw new IllegalArgumentException("Cannot start from empty storage");
        } else {
          doInitialize = false;
        }
        break;
      case "auto":
        doInitialize = emptyStorage;
        break;
      case "initial":
        if (!emptyStorage && !forceDBClean) {
          throw new IllegalArgumentException("Cannot initialize non-empty storage."
              + " Use --force-db-clean to clean automatically");
        }
        doInitialize = true;
        break;
      default:
        throw new IllegalArgumentException("Unsupported start-mode " + startMode);
    }

    if (doInitialize && !emptyStorage && forceDBClean) {
      db.close();
      try{
        dbFactory.removeDatabase(genesisTime, depositRoot);
      } catch (RuntimeException e) {
        throw new IllegalStateException("Cannot clean DB, remove files manually", e);
      }
      db = dbFactory.getOrCreateDatabase(genesisTime, depositRoot);
      beaconChainStorage = storageFactory.create(db);
    }

    if (doInitialize) {
      StorageUtils.initializeStorage(beaconChainStorage, spec, initialState);
    }

    NodeLauncher node = new NodeLauncher(
        specBuilder.buildSpec(),
        depositContract,
        credentials,
        libp2pLauncher, //connectionManager,
        db,
        beaconChainStorage,
        schedulers,
        true);

    if (cliOptions.isDumpTuples()) {
      BeaconTupleDetailsDumper dumper =
          new BeaconTupleDetailsDumper(
              "tuple_dump_" + config.getConfig().getName(), serializerFactory);
      try {
        dumper.init();
      } catch (IOException e) {
        throw new IllegalArgumentException("Couldn't initialize block dumper", e);
      }
      // dump genesis state
      try {
        dumper.dumpState("genesis", initialState);
      } catch (IOException e) {
        logger.error("Cannot dump state", e);
      }
      Flux.from(node.getBeaconChain().getBlockStatesStream())
          .subscribe(
              btd -> {
                try {
                  dumper.dump(btd);
                } catch (IOException e) {
                  logger.error("Cannot dump state", e);
                }
              });
    }

    node.start();

    Runtime.getRuntime().addShutdownHook(new Thread(node::stop));

    while (true) {
      try {
        Thread.sleep(1000000L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static class Builder {
    private MainConfig config;
    private Level logLevel = Level.INFO;
    private Node cliOptions;

    public Builder() {}

    public NodeCommandLauncher build() {
      assert config != null;

      ConfigBuilder<SpecData> specConfigBuilder =
          new ConfigBuilder<>(SpecData.class).addYamlConfigFromResources("/config/spec-constants.yml");

      if (cliOptions != null
          && cliOptions.getSpecConstantsFile() != null
          && cliOptions.getSpecConstantsFile().length() > 0) {
        SpecData chainSpec = config.getChainSpec();
        SpecConstantsDataMerged specConstantsYaml =
            loadSpecConstantsDataMerged(cliOptions.getSpecConstantsFile());
        SpecConstantsData mergedConstants =
            mergeSpecConstantsData(chainSpec.getSpecConstants(), specConstantsYaml);
        chainSpec.setSpecConstants(mergedConstants);
        config.setChainSpec(chainSpec);
      }
      if (config.getChainSpec().isDefined()) {
        specConfigBuilder.addConfig(config.getChainSpec());
      }

      SpecData spec = specConfigBuilder.build();

      SpecBuilder specBuilder = new SpecBuilder().withSpec(spec);

      if (cliOptions.getName() != null) {
        config.getConfig().setName(cliOptions.getName());
      }

      if (cliOptions.getInitialDepositCount() != null) {
        if (config.getConfig().getValidator().getContract() instanceof EmulatorContract) {
          EmulatorContract contract = (EmulatorContract) config.getConfig().getValidator().getContract();
          ValidatorKeys.InteropKeys keys = new ValidatorKeys.InteropKeys();
          keys.setCount(cliOptions.getInitialDepositCount());
          contract.setKeys(Collections.singletonList(keys));
        }
      }

      if (cliOptions.getListenPort() != null || cliOptions.getActivePeers() != null) {
        Libp2pNetwork network = (Libp2pNetwork) config
                .getConfig()
                .getNetworks()
                .stream()
                .filter(n -> n instanceof Libp2pNetwork)
                .findFirst().orElse(null);

        if (network == null) {
          network = new Libp2pNetwork();
          config.getConfig().getNetworks().add(network);
        }

        if (cliOptions.getListenPort() != null) {
          network.setListenPort(cliOptions.getListenPort());
        }

        if (cliOptions.getActivePeers() != null) {
          network.setActivePeers(cliOptions.getActivePeers());
        }
      }

      if (cliOptions.getValidators() != null) {
        List<String> validatorKeys = new ArrayList<>();
        List<KeyPair> depositKeypairs = null;
        for (String key : cliOptions.getValidators()) {
          if (key.startsWith("0x")) {
            validatorKeys.add(key);
          } else {
            if (depositKeypairs == null) {
              depositKeypairs = ConfigUtils
                  .createKeyPairs(((EmulatorContract)config.getConfig().getValidator().getContract()).getKeys());
            }
            List<KeyPair> finalDepositKeypairs = depositKeypairs;

            IntStream indices;
            if (key.contains("-")) {
              int idx = key.indexOf("-");
              int start = Integer.parseInt(key.substring(0, idx));
              int end = Integer.parseInt(key.substring(idx + 1));
              indices = IntStream.range(start, end + 1);
            } else {
              indices = IntStream.of(Integer.parseInt(key));
            }
            indices
                .mapToObj(i -> finalDepositKeypairs.get(i).getPrivate().getEncodedBytes().toString())
                .forEach(validatorKeys::add);
          }
        }
        Insecure signer = new Insecure();
        signer.setKeys(Collections.singletonList(new Private(validatorKeys)));
        config.getConfig().getValidator().setSigner(signer);
      }

      if (cliOptions.getGenesisTime() != null) {
        SimpleDateFormat[]  supportedFormats = new SimpleDateFormat[] {
            new SimpleDateFormat("yyyy-MM-dd HH:mm"),
            new SimpleDateFormat("HH:mm")};

        Date time = null;
        for (SimpleDateFormat format : supportedFormats) {
          format.setTimeZone(TimeZone.getTimeZone("GMT"));
          try {
            time = format.parse(cliOptions.getGenesisTime());
            break;
          } catch (ParseException e) {
            continue;
          }
        }
        if (time == null) {
          throw new ConfigException(
              "Couldn't parse --genesisTime option value: '" + cliOptions.getGenesisTime() + "'");
        }
        if (time.getYear() + 1900 == 1970) {
          Date now = new Date();
          time.setYear(now.getYear());
          time.setMonth(now.getMonth());
          time.setDate(now.getDate());
        }

        if (!(config.getConfig().getValidator().getContract() instanceof EmulatorContract)) {
          throw new ConfigException("Genesis time can only be set for 'emulator' contract type");
        }
        EmulatorContract contract = (EmulatorContract) config.getConfig().getValidator().getContract();
        contract.setGenesisTime(time);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        logger.info("Genesis time from cli option: "
            + format.format(time) + " GMT");
      }

      if (config.getConfig().getValidator().getContract() instanceof EmulatorContract) {
        EmulatorContract contract = (EmulatorContract) config.getConfig().getValidator().getContract();
        if (contract.getGenesisTime() == null) {
          Date defaultTime = new Date();
          defaultTime.setMinutes(0);
          defaultTime.setSeconds(0);
          defaultTime = new Date(defaultTime.getTime() / 1000 * 1000);
          contract.setGenesisTime(defaultTime);

          SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          format.setTimeZone(TimeZone.getTimeZone("GMT"));
          logger.warn("Genesis time not specified. Default genesisTime was generated: "
                  + format.format(defaultTime) + " GMT");
        }
      }

      if (cliOptions.getMetricsEndpoint() != null) {
        config.getConfig().setMetricsEndpoint(cliOptions.getMetricsEndpoint());
      }

      if (cliOptions.getDbPrefix() != null) {
        config.getConfig().setDb(cliOptions.getDbPrefix());
      }

      return new NodeCommandLauncher(
        config,
        specBuilder,
        cliOptions,
        logLevel);
    }

    @NotNull
    private static SpecConstantsData mergeSpecConstantsData(SpecConstantsData specConsts, SpecConstantsDataMerged specConstsYaml) {
      if (specConsts == null) {
        return specConstsYaml;
      } else {
        try {
          return Objects.copyProperties(specConsts, specConstsYaml);
        } catch (IllegalAccessException| InvocationTargetException e) {
          throw new RuntimeException(
              String.format("Failed to merge config %s into main config", specConsts), e);
        }
      }
    }

    private static SpecConstantsDataMerged loadSpecConstantsDataMerged(String specConstants) {
      ConfigBuilder<SpecConstantsDataMerged> specConstsBuilder =
          new ConfigBuilder<>(SpecConstantsDataMerged.class);
      if ("minimal".equals(specConstants)) {
        specConstsBuilder.addYamlConfigFromResources("/spec/" + specConstants + ".yaml");
      } else {
        specConstsBuilder.addYamlConfig(Paths.get(specConstants).toFile());
      }
      return specConstsBuilder.build();
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

    public Builder withCliOptions(Node node) {
      cliOptions = node;
      return this;
    }
  }
}

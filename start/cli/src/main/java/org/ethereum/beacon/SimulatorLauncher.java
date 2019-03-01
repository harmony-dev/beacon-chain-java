package org.ethereum.beacon;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.action.Action;
import org.ethereum.beacon.emulator.config.main.action.ActionSimulate;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.LoggerMDCExecutor;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.util.SimulateUtils;
import org.ethereum.beacon.wire.LocalWireHub;
import org.ethereum.beacon.wire.WireApi;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

public class SimulatorLauncher implements Runnable {
  private static final Logger logger = LogManager.getLogger(SimulatorLauncher.class);
  private static final Logger logPeer = logger; //LogManager.getLogger("peer");

  private final ActionSimulate simulateConfig;
  private final MainConfig mainConfig;
  private final ChainSpec chainSpec;
  private final SpecHelpers specHelpers;
  private final Level logLevel;
  private final Consumer<MainConfig> onUpdateConfig;

  /**
   * Creates Simulator launcher with following settings
   *
   * @param mainConfig Configuration and run plan
   * @param specHelpers Chain specification
   * @param logLevel Log level, Apache log4j type
   * @param onUpdateConfig Callback to run when mainConfig is updated
   */
  public SimulatorLauncher(
      MainConfig mainConfig,
      SpecHelpers specHelpers,
      Level logLevel,
      Consumer<MainConfig> onUpdateConfig) {
    this.mainConfig = mainConfig;
    this.chainSpec = specHelpers.getChainSpec();
    this.specHelpers = specHelpers;
    List<Action> actions = mainConfig.getPlan().getValidator();
    Optional<ActionSimulate> actionSimulate =
        actions.stream()
            .filter(a -> a instanceof ActionSimulate)
            .map(a -> (ActionSimulate) a)
            .findFirst();
    if (!actionSimulate.isPresent()) {
      throw new RuntimeException("Simulate settings are not set");
    }
    this.simulateConfig = actionSimulate.get();
    if (simulateConfig.getCount() == null && simulateConfig.getPrivateKeys() == null) {
      throw new RuntimeException("Set either number of validators or private keys.");
    }
    this.logLevel = logLevel;
    this.onUpdateConfig = onUpdateConfig;
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
      LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
      loggerConfig.setLevel(logLevel);
      context.updateLoggers();
    }
  }

  private void onUpdateConfig() {
    this.onUpdateConfig.accept(mainConfig);
  }

  private Pair<List<Deposit>, List<BLS381.KeyPair>> getValidatorDeposits() {
    if (simulateConfig.getPrivateKeys() != null && !simulateConfig.getPrivateKeys().isEmpty()) {
      List<BLS381.KeyPair> keyPairs = new ArrayList<>();
      for (String pKey : simulateConfig.getPrivateKeys()) {
        keyPairs.add(BLS381.KeyPair.create(BLS381.PrivateKey.create(Bytes32.fromHexString(pKey))));
      }
      return Pair.with(SimulateUtils.getDepositsForKeyPairs(keyPairs, specHelpers), keyPairs);
    } else {
      Pair<List<Deposit>, List<BLS381.KeyPair>> anyDeposits =
          SimulateUtils.getAnyDeposits(specHelpers, simulateConfig.getCount());
      List<String> pKeysEncoded = new ArrayList<>();
      anyDeposits
          .getValue1()
          .forEach(
              pk -> {
                pKeysEncoded.add(pk.getPrivate().getEncodedBytes().toString());
              });
      simulateConfig.setPrivateKeys(pKeysEncoded);
      onUpdateConfig();
      return anyDeposits;
    }
  }

  public void run() {
    setupLogging();
    Pair<List<Deposit>, List<BLS381.KeyPair>> validatorDeposits = getValidatorDeposits();
    List<Deposit> deposits = validatorDeposits.getValue0();
    List<BLS381.KeyPair> keyPairs = validatorDeposits.getValue1();

    Random rnd = new Random(1);
    Time genesisTime = Time.of(10 * 60);

    MDCControlledSchedulers controlledSchedulers = new MDCControlledSchedulers();
    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);

    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));

    LocalWireHub localWireHub = new LocalWireHub(s -> {});
    DepositContract.ChainStart chainStart =
        new DepositContract.ChainStart(genesisTime, eth1Data, deposits);
    DepositContract depositContract = new SimpleDepositContract(chainStart);

    List<Launcher> peers = new ArrayList<>();

    logger.info("Creating validators...");
    for (int i = 0; i < keyPairs.size(); i++) {
      ControlledSchedulers schedulers = controlledSchedulers.createNew("" + i);
      WireApi wireApi = localWireHub.createNewPeer("" + i);

      Launcher launcher =
          new Launcher(
              specHelpers,
              depositContract,
              keyPairs.get(i),
              wireApi,
              new MemBeaconChainStorageFactory(),
              schedulers);

      peers.add(launcher);
    }
    logger.info("Validators created");

    for (int i = 0; i < peers.size(); i++) {
      Launcher launcher = peers.get(i);

      Flux.from(launcher.slotTicker.getTickerStream()).subscribe(slot ->
          logPeer.debug("New slot: " + slot.toString(chainSpec, genesisTime)));
      Flux.from(launcher.observableStateProcessor.getObservableStateStream())
          .subscribe(os -> logPeer.debug("New observable state: " + os.toString(specHelpers)));
      Flux.from(launcher.beaconChainValidator.getProposedBlocksStream())
          .subscribe(block -> logPeer.info("New block created: "
              + block.toString(chainSpec, genesisTime, specHelpers::hash_tree_root)));
      Flux.from(launcher.beaconChainValidator.getAttestationsStream())
          .subscribe(attest -> logPeer.info("New attestation created: "
              + attest.toString(chainSpec, genesisTime)));
      Flux.from(launcher.beaconChain.getBlockStatesStream())
          .subscribe(blockState -> logPeer.debug("Block imported: "
              + blockState.getBlock().toString(chainSpec, genesisTime, specHelpers::hash_tree_root)));
    }

    while (true) {
      controlledSchedulers.addTime(Duration.ofSeconds(10));
    }
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

  static class MDCControlledSchedulers {
    private DateFormat localTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    private List<ControlledSchedulers> schedulersList = new ArrayList<>();
    private long currentTime;

    public ControlledSchedulers createNew(String validatorId) {
      ControlledSchedulers[] newSched = new ControlledSchedulers[1];
      LoggerMDCExecutor mdcExecutor = new LoggerMDCExecutor()
          .add("validatorTime", () -> localTimeFormat.format(new Date(newSched[0].getCurrentTime())))
          .add("validatorIndex", () -> "" + validatorId);
      newSched[0] = Schedulers.createControlled(() -> mdcExecutor);
      newSched[0].setCurrentTime(currentTime);
      schedulersList.add(newSched[0]);

      return newSched[0];
    }

    public void setCurrentTime(long time) {
      currentTime = time;
      schedulersList.forEach(cs -> cs.setCurrentTime(time));
    }

    void addTime(Duration duration) {
      addTime(duration.toMillis());
    }

    void addTime(long millis) {
      setCurrentTime(currentTime + millis);
    }
  }
}

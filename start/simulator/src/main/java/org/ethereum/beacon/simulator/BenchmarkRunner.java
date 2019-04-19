package org.ethereum.beacon.simulator;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.ethereum.beacon.Launcher;
import org.ethereum.beacon.bench.BenchmarkController;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.util.BlsKeyPairReader;
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

public class BenchmarkRunner implements Runnable {
  private static final Logger logger = LogManager.getLogger("benchmaker");

  private final int epochCount;
  private final int validatorCount;
  private final BeaconChainSpec spec;
  private final BeaconChainSpec.Builder specBuilder;

  public BenchmarkRunner(
      int epochCount, int validatorCount, BeaconChainSpec.Builder specBuilder) {
    this.epochCount = epochCount;
    this.validatorCount = validatorCount;
    this.specBuilder = specBuilder;
    this.spec = specBuilder.build();
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

    Pair<List<Deposit>, List<KeyPair>> validatorDeposits =
        getValidatorDeposits(spec, validatorCount);

    List<Deposit> deposits = validatorDeposits.getValue0().stream()
        .filter(Objects::nonNull).collect(Collectors.toList());
    List<KeyPair> keyPairs = validatorDeposits.getValue1();

    MDCControlledSchedulers controlledSchedulers = new MDCControlledSchedulers();
    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);

    Eth1Data eth1Data = new Eth1Data(Hash32.ZERO, Hash32.ZERO);

    LocalWireHub localWireHub =
        new LocalWireHub(s -> {}, controlledSchedulers.createNew("wire"));
    DepositContract.ChainStart chainStart =
        new DepositContract.ChainStart(genesisTime, eth1Data, deposits);
    DepositContract depositContract = new SimpleDepositContract(chainStart);

    logger.info("Bootstrapping validators...");
    TimeCollector proposeTimeCollector = new TimeCollector();

    ControlledSchedulers schedulers = controlledSchedulers.createNew("V0");
    WireApi wireApi = localWireHub.createNewPeer("0");

    List<BLS381Credentials> blsCreds;
    if (spec.isBlsVerify()) {
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

    BenchmarkController benchmarkController = BenchmarkController.newInstance();
    Launcher instance =
        new Launcher(
            specBuilder.build(),
            depositContract,
            blsCreds,
            wireApi,
            new MemBeaconChainStorageFactory(),
            schedulers,
            proposeTimeCollector,
            benchmarkController);

    List<SlotNumber> slots = new ArrayList<>();
    List<Attestation> attestations = new ArrayList<>();
    List<BeaconBlock> blocks = new ArrayList<>();

    Flux.from(instance.getSlotTicker().getTickerStream()).subscribe(slots::add);
    Flux.from(instance.getValidatorService().getAttestationsStream())
        .publishOn(instance.getSchedulers().reactorEvents())
        .subscribe(attestations::add);
    Flux.from(instance.getBeaconChain().getBlockStatesStream())
        .subscribe(blockState -> blocks.add(blockState.getBlock()));

    logger.info("Time starts running ...");
    controlledSchedulers.setCurrentTime(
        genesisTime.plus(spec.getConstants().getSecondsPerSlot()).getMillis().getValue() - 9);

    // skip 1st epoch, add extra slot to trigger last epoch transition
    int slotsToRun = (epochCount + 1) * spec.getConstants().getSlotsPerEpoch().getIntValue();
    for (int i = 0; i < slotsToRun; i++) {
      benchmarkController.onBeforeNewSlot(spec.getConstants().getGenesisSlot().plus(i));

      controlledSchedulers.addTime(
          Duration.ofMillis(spec.getConstants().getSecondsPerSlot().getMillis().getValue()));

      if (slots.size() > 1) {
        logger.warn("More than 1 slot generated: " + slots);
      }
      if (slots.isEmpty()) {
        logger.error("No slots generated");
      }

      logger.info("Slot " + slots.get(0).toStringNumber(spec.getConstants())
          + ", blocks: " + blocks.size()
          + ", attestations: " + attestations.size());

      slots.clear();
      attestations.clear();
      blocks.clear();
    }

    System.out.println();
    System.out.println(benchmarkController.createReport().print());
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

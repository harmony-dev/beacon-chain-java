package org.ethereum.beacon.benchmaker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.bench.BenchmarkController;
import org.ethereum.beacon.bench.BenchmarkReport;
import org.ethereum.beacon.bench.BenchmarkUtils;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.consensus.util.CachingBeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
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
import org.ethereum.beacon.start.common.Launcher;
import org.ethereum.beacon.start.common.util.MDCControlledSchedulers;
import org.ethereum.beacon.start.common.util.SimpleDepositContract;
import org.ethereum.beacon.start.common.util.SimulateUtils;
import org.ethereum.beacon.util.stats.MeasurementsCollector;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.wire.LocalWireHub;
import org.ethereum.beacon.wire.WireApiSub;
import org.javatuples.Pair;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class BenchmarkRunner implements Runnable {
  private static final Logger logger = LogManager.getLogger("benchmaker");

  private final int epochCount;
  private final int validatorCount;
  private final int warmUpEpochs;
  private final BeaconChainSpec spec;
  private final BeaconChainSpec.Builder specBuilder;

  public BenchmarkRunner(
      int epochCount, int validatorCount, BeaconChainSpec.Builder specBuilder, int warmUpEpochs) {
    this.epochCount = epochCount;
    this.validatorCount = validatorCount;
    this.specBuilder = specBuilder;
    this.spec = specBuilder.build();
    this.warmUpEpochs = warmUpEpochs;
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
    logger.info(
        "Benchmark session: {} epochs, {} validators, BLS is {}, caches are {}",
        epochCount,
        validatorCount,
        spec.isBlsVerify() ? "enabled" : "disabled",
        ((CachingBeaconChainSpec) spec).isCacheEnabled() ? "enabled" : "disabled");

    Time genesisTime = Time.ZERO;

    Pair<List<Deposit>, List<KeyPair>> validatorDeposits =
        getValidatorDeposits(spec, validatorCount);

    List<Deposit> deposits = validatorDeposits.getValue0().stream()
        .filter(Objects::nonNull).collect(Collectors.toList());
    List<KeyPair> keyPairs = validatorDeposits.getValue1();

    MDCControlledSchedulers controlledSchedulers = new MDCControlledSchedulers();
    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() - 1000);

    Eth1Data eth1Data = new Eth1Data(Hash32.ZERO, UInt64.valueOf(deposits.size()), Hash32.ZERO);

    LocalWireHub localWireHub =
        new LocalWireHub(s -> {}, controlledSchedulers.createNew("wire"));
    ChainStart chainStart =
        new ChainStart(genesisTime, eth1Data, deposits);
    DepositContract depositContract =
        new SimpleDepositContract(chainStart, controlledSchedulers.createNew("chainStart"));

    logger.info("Bootstrapping validators ...");

    ControlledSchedulers schedulers = controlledSchedulers.createNew("V0");
    WireApiSub wireApi = localWireHub.createNewPeer("0");

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

    BenchmarkController benchmarkController = BenchmarkController.newInstance(warmUpEpochs);
    Launcher instance =
        new Launcher(
            specBuilder.build(),
            depositContract,
            blsCreds,
            wireApi,
            new MemBeaconChainStorageFactory(spec.getObjectHasher()),
            schedulers,
            benchmarkController);

    List<SlotNumber> slots = new ArrayList<>();
    List<Attestation> attestations = new ArrayList<>();
    List<SignedBeaconBlock> blocks = new ArrayList<>();

    Flux.from(instance.getSlotTicker().getTickerStream()).subscribe(slots::add);
    Flux.from(instance.getValidatorService().getAttestationsStream())
        .publishOn(instance.getSchedulers().events().toReactor())
        .subscribe(attestations::add);
    Flux.from(instance.getBeaconChain().getBlockStatesStream())
        .subscribe(blockState -> blocks.add(blockState.getBlock()));

    // show benchmark report if process exit requested by user
    Runtime.getRuntime()
        .addShutdownHook(new Thread(() -> printReport(instance, benchmarkController)));

    logger.info("Time starts running ...");
    controlledSchedulers.setCurrentTime(
        genesisTime.plus(spec.getConstants().getSecondsPerSlot()).getMillis().getValue() - 9);

    // skip 1st epoch, add extra slot to trigger last epoch transition
    int slotsToRun =
        (epochCount + benchmarkController.getWarmUpEpochs().getIntValue())
            * spec.getConstants().getSlotsPerEpoch().getIntValue();
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
  }

  private void printReport(Launcher instance, BenchmarkController controller) {
    System.out.println();
    System.out.println(printOverview(instance));

    System.out.println();
    System.out.println(controller.createReport().print());
  }

  private String printOverview(Launcher instance) {
    return Stats.format("PROCESSING OVERVIEW", "min, ms", "avg, ms", "95%, ms") + '\n'
        + Stats.createFrom(instance.getSlotCollector()).print("slot", "  ") + '\n'
        + Stats.createFrom(instance.getBlockCollector()).print("block", "  ") + '\n'
        + Stats.createFrom(instance.getEpochCollector()).print("epoch", "  ") + '\n';
  }

  private static class Stats {
    private long minTime = 0;
    private double avgTime = 0;
    private long percentile = 0;

    private Stats() {}

    String print(String title, String leftPadding) {
      return format(
          leftPadding + title,
          String.format("%.3f", minTime / 1_000_000d),
          String.format("%.3f", avgTime / 1_000_000d),
          String.format("%.3f", percentile / 1_000_000d));
    }

    static String format(String title, String minTime, String avgTime, String percentile) {
      return String.format("%-45s%15s%15s%15s", title, minTime, avgTime, percentile);
    }

    static Stats createFrom(MeasurementsCollector collector) {
      Stats stats = new Stats();
      if (collector.getMeasurements().isEmpty()) {
        return stats;
      }

      List<Long> sortedMeasurements =
          collector.getMeasurements().stream().sorted().collect(Collectors.toList());
      stats.minTime = sortedMeasurements.get(0);
      stats.avgTime = collector.getAvg();
      stats.percentile =
          BenchmarkUtils.percentile(BenchmarkReport.PERCENTILE_RATIO, sortedMeasurements);

      return stats;
    }
  }
}

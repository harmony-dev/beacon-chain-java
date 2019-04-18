package org.ethereum.beacon.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.ethereum.beacon.bench.BenchmarkReport.Builder;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.util.stats.TimeCollector;
import tech.pegasys.artemis.util.uint.UInt64;

public class BenchmarkControllerImpl implements BenchmarkController {

  private final List<BenchInstance> benchingInstances = new ArrayList<>();

  @Override
  public BeaconChainSpec wrap(BenchmarkRoutine routine, BeaconChainSpec spec) {
    BenchInstance newInstance = BenchInstance.create(spec, routine, spec.getConstants());
    benchingInstances.add(newInstance);
    return newInstance.specWrapper;
  }

  @Override
  public BenchmarkReport createReport() {
    Builder reportBuilder = new BenchmarkReport.Builder();
    benchingInstances.forEach(
        instance -> {
          // create last checkpoint and add to builder
          instance.createCheckpoint(SlotNumber.castFrom(UInt64.MAX_VALUE));
          reportBuilder.addRoutine(instance.routine, instance.measurements);
        });
    return reportBuilder.build();
  }

  @Override
  public void onBeforeNewSlot(SlotNumber slot) {
    benchingInstances.forEach(instance -> instance.onBeforeNewSlot(slot));
  }

  private static class BenchInstance {
    final BenchmarkRoutine routine;
    final BenchmarkingBeaconChainSpec specWrapper;
    final SlotNumber startSlot;
    final SlotNumber measurementPeriod;
    final List<Map<String, TimeCollector>> measurements = new ArrayList<>();

    SlotNumber previousCheckpoint = SlotNumber.castFrom(UInt64.MAX_VALUE);

    private BenchInstance(
        BenchmarkRoutine routine,
        BenchmarkingBeaconChainSpec specWrapper,
        SlotNumber startSlot,
        SlotNumber measurementPeriod) {
      this.routine = routine;
      this.specWrapper = specWrapper;
      this.startSlot = startSlot;
      this.measurementPeriod = measurementPeriod;
    }

    static BenchInstance create(
        BeaconChainSpec spec, BenchmarkRoutine routine, SpecConstants constants) {
      switch (routine) {
        case SLOT:
        case BLOCK:
          // start tracking slots and blocks since the beginning of the 2nd epoch
          return new BenchInstance(
              routine,
              BenchmarkingBeaconChainSpec.wrap(spec),
              constants.getGenesisSlot().plus(constants.getSlotsPerEpoch()),
              SlotNumber.of(1));
        case EPOCH:
          // start tracking epochs when first epoch transition has happened
          return new BenchInstance(
              routine,
              BenchmarkingBeaconChainSpec.wrap(spec),
              constants.getGenesisSlot().plus(constants.getSlotsPerEpoch()).increment(),
              constants.getSlotsPerEpoch());
        default:
          throw new IllegalArgumentException("Unsupported benchmark routine: " + routine);
      }
    }

    void onBeforeNewSlot(SlotNumber slot) {
      if (previousCheckpoint.equals(UInt64.MAX_VALUE) && startSlot.lessEqual(slot)) {
        startTracking(slot);
      }
      if (!previousCheckpoint.equals(UInt64.MAX_VALUE)
          && previousCheckpoint.plus(measurementPeriod).lessEqual(slot)) {
        createCheckpoint(slot);
      }
    }

    void startTracking(SlotNumber slot) {
      specWrapper.startTracking();
      previousCheckpoint = slot;
    }

    void createCheckpoint(SlotNumber slot) {
      measurements.add(specWrapper.drainCollectors());
      previousCheckpoint = slot;
    }
  }
}

package org.ethereum.beacon.bench;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.util.stats.MeasurementsCollector;

public interface BenchmarkController {

  enum BenchmarkRoutine {
    SLOT,
    BLOCK,
    EPOCH;

    public String print() {
      return String.format("%s processing", this.name()).toUpperCase();
    }
  }

  BenchmarkController NO_BENCHES = new BenchmarkController() {};

  default EpochNumber getWarmUpEpochs() {
    return EpochNumber.ZERO;
  }

  default BeaconChainSpec wrap(BenchmarkRoutine routine, BeaconChainSpec spec) {
    return spec;
  }

  default BenchmarkReport createReport() {
    return new BenchmarkReport();
  }

  default void onBeforeNewSlot(SlotNumber slot) {}

  static BenchmarkController newInstance(int warmUpEpochs) {
    return new BenchmarkControllerImpl(EpochNumber.of(warmUpEpochs));
  }
}

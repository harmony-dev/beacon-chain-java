package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.CheckedAttestation;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.stream.AbstractDelegateProcessor;

public class TimeFrameProcessor extends AbstractDelegateProcessor<Input, CheckedAttestation> {

  private final TimeFrameFilter filter;

  public TimeFrameProcessor(TimeFrameFilter filter) {
    this.filter = filter;
  }

  @Override
  protected void hookOnNext(Input value) {
    if (value.getType().equals(Checkpoint.class)) {
      filter.feedFinalizedCheckpoint(value.unbox());
    } else if (value.getType().equals(SlotNumber.class)) {
      filter.feedNewSlot(value.unbox());
    } else if (value.getType().equals(ReceivedAttestation.class)) {
      if (filter.isInitialized()) {
        publishOut(new CheckedAttestation(filter.check(value.unbox()), value.unbox()));
      }
    } else {
      throw new IllegalArgumentException(
          "Unsupported input type: " + value.getType().getSimpleName());
    }
  }
}

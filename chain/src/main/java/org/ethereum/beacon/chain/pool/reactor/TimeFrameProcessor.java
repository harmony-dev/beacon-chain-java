package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.CheckedAttestation;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.stream.AbstractDelegateProcessor;

/**
 * Processor throttling attestations through a {@link TimeFrameFilter}.
 *
 * <p>Input:
 *
 * <ul>
 *   <li>recently finalized checkpoints.
 *   <li>new slots.
 *   <li>attestations.
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>attestations tagged with the check flag.
 * </ul>
 */
public class TimeFrameProcessor extends AbstractDelegateProcessor<Object, CheckedAttestation> {

  private final TimeFrameFilter filter;

  public TimeFrameProcessor(TimeFrameFilter filter) {
    this.filter = filter;
  }

  @Override
  protected void hookOnNext(Object value) {
    if (value.getClass().equals(Checkpoint.class)) {
      filter.feedFinalizedCheckpoint((Checkpoint) value);
    } else if (value.getClass().equals(SlotNumber.class)) {
      filter.feedNewSlot((SlotNumber) value);
    } else if (value.getClass().equals(ReceivedAttestation.class)) {
      if (filter.isInitialized()) {
        ReceivedAttestation attestation = (ReceivedAttestation) value;
        publishOut(new CheckedAttestation(filter.check(attestation), attestation));
      }
    } else {
      throw new IllegalArgumentException(
          "Unsupported input type: " + value.getClass().getSimpleName());
    }
  }
}

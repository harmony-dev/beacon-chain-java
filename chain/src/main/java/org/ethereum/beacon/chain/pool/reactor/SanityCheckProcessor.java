package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.CheckedAttestation;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.stream.AbstractDelegateProcessor;

public class SanityCheckProcessor extends AbstractDelegateProcessor<Input, CheckedAttestation> {

  private final SanityChecker checker;

  public SanityCheckProcessor(SanityChecker checker) {
    this.checker = checker;
  }

  @Override
  protected void hookOnNext(Input value) {
    if (value.getType().equals(Checkpoint.class)) {
      checker.feedFinalizedCheckpoint(value.unbox());
    } else if (value.getType().equals(SlotNumber.class)) {
      checker.feedNewSlot(value.unbox());
    } else if (value.getType().equals(ReceivedAttestation.class)) {
      if (checker.isInitialized()) {
        publishOut(new CheckedAttestation(checker.check(value.unbox()), value.unbox()));
      }
    } else {
      throw new IllegalArgumentException(
          "Unsupported input type: " + value.getType().getSimpleName());
    }
  }
}

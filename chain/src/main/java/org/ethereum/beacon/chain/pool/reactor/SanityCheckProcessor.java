package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.CheckedAttestation;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.stream.AbstractDelegateProcessor;

/**
 * Processor throttling attestations through a {@link SanityChecker}.
 *
 * <p>Input:
 *
 * <ul>
 *   <li>recently finalized checkpoints.
 *   <li>attestations.
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>attestations tagged with the check flag.
 * </ul>
 */
public class SanityCheckProcessor extends AbstractDelegateProcessor<Input, CheckedAttestation> {

  private final SanityChecker checker;

  public SanityCheckProcessor(SanityChecker checker) {
    this.checker = checker;
  }

  @Override
  protected void hookOnNext(Input value) {
    if (value.getType().equals(Checkpoint.class)) {
      checker.feedFinalizedCheckpoint(value.unbox());
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

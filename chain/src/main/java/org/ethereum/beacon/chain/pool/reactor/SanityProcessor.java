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
public class SanityProcessor extends AbstractDelegateProcessor<Object, CheckedAttestation> {

  private final SanityChecker checker;

  public SanityProcessor(SanityChecker checker) {
    this.checker = checker;
  }

  @Override
  protected void hookOnNext(Object value) {
    if (value.getClass().equals(Checkpoint.class)) {
      checker.feedFinalizedCheckpoint((Checkpoint) value);
    } else if (value.getClass().equals(ReceivedAttestation.class)) {
      if (checker.isInitialized()) {
        ReceivedAttestation attestation = (ReceivedAttestation) value;
        publishOut(new CheckedAttestation(checker.check(attestation), attestation));
      }
    } else {
      throw new IllegalArgumentException(
          "Unsupported input type: " + value.getClass().getSimpleName());
    }
  }
}

package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

/** A common interface for various {@link BeaconBlock} verifications defined by the spec. */
public interface BeaconBlockVerifier {

  /**
   * Runs block verifications.
   *
   * @param block a block to verify.
   * @param state a state which slot number is equal to {@code block.getSlot()} produced by per-slot
   *     processing.
   * @return result of the verifications.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#per-slot-processing">Per-slot
   *     processing</a> in the spec.
   */
  VerificationResult verify(BeaconBlock block, BeaconState state);
}

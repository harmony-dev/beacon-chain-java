package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.core.BeaconState;

/**
 * Interface to verify various beacon chain operations.
 *
 * @param <T> an operation type.
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#operations">Operations</a>
 *     in the spec.
 */
public interface OperationVerifier<T> {

  /**
   * Runs operation verifications.
   *
   * @param operation an operation to verify.
   * @param state a state produced by per-slot processing which {@code slot} is equal to the slot of
   *     the block which operation does belong to.
   * @return result of the verifications.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#per-slot-processing">Per-slot
   *     processing</a> in the spec.
   */
  VerificationResult verify(T operation, BeaconState state);
}

package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.core.BeaconBlockBody;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Requests a quit from validator registry.
 *
 * @see BeaconBlockBody
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#exit">Exit
 *     in the spec</a>
 */
public class Exit {

  /** Minimum slot for processing exit. */
  private final UInt64 slot;
  /** Index of the exiting validator. */
  private final UInt24 validatorIndex;
  /** Validator signature. */
  private final Bytes96 signature;

  public Exit(UInt64 slot, UInt24 validatorIndex, Bytes96 signature) {
    this.slot = slot;
    this.validatorIndex = validatorIndex;
    this.signature = signature;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public UInt24 getValidatorIndex() {
    return validatorIndex;
  }

  public Bytes96 getSignature() {
    return signature;
  }
}

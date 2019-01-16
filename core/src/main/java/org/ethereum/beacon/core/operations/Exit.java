package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
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
@SSZSerializable
public class Exit {

  /** Minimum slot for processing exit. */
  @SSZ
  private final UInt64 slot;
  /** Index of the exiting validator. */
  @SSZ
  private final UInt24 validatorIndex;
  /** Validator signature. */
  @SSZ
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Exit exit = (Exit) o;
    return slot.equals(exit.slot) &&
        validatorIndex.equals(exit.validatorIndex) &&
        signature.equals(exit.signature);
  }
}

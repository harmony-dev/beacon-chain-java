package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.util.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.Objects;

@SSZSerializable
public class Exit {
  private final UInt64 slot;
  private final UInt24 validatorIndex;
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

  @Override
  public int hashCode() {
    return Objects.hash(slot, validatorIndex, signature);
  }
}

package org.ethereum.beacon.core.operations;

import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

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
}

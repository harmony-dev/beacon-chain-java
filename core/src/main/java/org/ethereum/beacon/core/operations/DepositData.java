package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.util.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class DepositData {
  private final DepositInput depositInput;
  private final UInt64 value;
  private final UInt64 timestamp;

  public DepositData(DepositInput depositInput, UInt64 value, UInt64 timestamp) {
    this.depositInput = depositInput;
    this.value = value;
    this.timestamp = timestamp;
  }

  public DepositInput getDepositInput() {
    return depositInput;
  }

  public UInt64 getValue() {
    return value;
  }

  public UInt64 getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DepositData that = (DepositData) o;
    return depositInput.equals(that.depositInput) &&
        value.equals(that.value) &&
        timestamp.equals(that.timestamp);
  }
}

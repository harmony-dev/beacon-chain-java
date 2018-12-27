package org.ethereum.beacon.core.operations;

import tech.pegasys.artemis.util.uint.UInt64;

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
}

package org.ethereum.beacon.core.operations.deposit;

import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
/**
 * A data of validator registration deposit.
 *
 * @see Deposit
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#depositdata">DepositData
 *     in the spec</a>
 */
public class DepositData {

  /** Deposit parameters. */
  private final DepositInput depositInput;
  /** Value in Gwei. */
  private final UInt64 value;
  /** Timestamp from deposit contract. */
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

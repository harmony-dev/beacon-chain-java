package org.ethereum.beacon.core.operations.deposit;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * A data of validator registration deposit.
 *
 * @see Deposit
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#depositdata">DepositData
 *     in the spec</a>
 */
@SSZSerializable
public class DepositData {

  /** Value in Gwei. */
  @SSZ private final Gwei amount;
  /** Timestamp from deposit contract. */
  @SSZ private final Time timestamp;
  /** Deposit parameters. */
  @SSZ private final DepositInput depositInput;

  public DepositData(Gwei amount, Time timestamp, DepositInput depositInput) {
    this.amount = amount;
    this.timestamp = timestamp;
    this.depositInput = depositInput;
  }

  public Gwei getValue() {
    return amount;
  }

  public Time getTimestamp() {
    return timestamp;
  }

  public DepositInput getDepositInput() {
    return depositInput;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DepositData that = (DepositData) o;
    return Objects.equal(depositInput, that.depositInput)
        && Objects.equal(amount, that.amount)
        && Objects.equal(timestamp, that.timestamp);
  }
}

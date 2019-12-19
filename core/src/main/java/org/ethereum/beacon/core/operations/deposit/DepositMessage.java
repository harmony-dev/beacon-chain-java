package org.ethereum.beacon.core.operations.deposit;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class DepositMessage {

  public static DepositMessage from(DepositData data) {
    return new DepositMessage(data.getPubKey(), data.getWithdrawalCredentials(), data.getAmount());
  }

  /** BLS public key. */
  @SSZ private final BLSPubkey pubKey;
  /** Withdrawal credentials. */
  @SSZ private final Hash32 withdrawalCredentials;
  /** Amount in Gwei. */
  @SSZ private final Gwei amount;

  public DepositMessage(BLSPubkey pubKey, Hash32 withdrawalCredentials, Gwei amount) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.amount = amount;
  }

  public BLSPubkey getPubKey() {
    return pubKey;
  }

  public Hash32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  public Gwei getAmount() {
    return amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DepositMessage that = (DepositMessage) o;
    return Objects.equal(pubKey, that.pubKey)
        && Objects.equal(withdrawalCredentials, that.withdrawalCredentials)
        && Objects.equal(amount, that.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(pubKey, withdrawalCredentials, amount);
  }
}

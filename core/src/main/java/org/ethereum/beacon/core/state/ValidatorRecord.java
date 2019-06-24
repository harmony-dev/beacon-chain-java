package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * A record denoting a validator in the validator registry.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/core/0_beacon-chain.md#validator">Validator
 *     </a>in the spec.
 */
@SSZSerializable
public class ValidatorRecord {

  /** BLS public key. */
  @SSZ private final BLSPubkey pubKey;
  /** Withdrawal credentials. */
  @SSZ private final Hash32 withdrawalCredentials;
  /** Epoch when became eligible for activation. */
  @SSZ private final EpochNumber activationEligibilityEpoch;
  /** Slot when validator activated */
  @SSZ private final EpochNumber activationEpoch;
  /** Slot when validator exited */
  @SSZ private final EpochNumber exitEpoch;
  /** Epoch when validator is eligible to withdraw */
  @SSZ private final EpochNumber withdrawableEpoch;
  /** Status flags. */
  @SSZ private final Boolean slashed;
  /** Effective balance. */
  @SSZ private final Gwei effectiveBalance;

  public ValidatorRecord(
      BLSPubkey pubKey,
      Hash32 withdrawalCredentials,
      EpochNumber activationEligibilityEpoch,
      EpochNumber activationEpoch,
      EpochNumber exitEpoch,
      EpochNumber withdrawableEpoch,
      Boolean slashed,
      Gwei effectiveBalance) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.activationEligibilityEpoch = activationEligibilityEpoch;
    this.activationEpoch = activationEpoch;
    this.exitEpoch = exitEpoch;
    this.withdrawableEpoch = withdrawableEpoch;
    this.slashed = slashed;
    this.effectiveBalance = effectiveBalance;
  }

  public BLSPubkey getPubKey() {
    return pubKey;
  }

  public Hash32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  public EpochNumber getActivationEligibilityEpoch() {
    return activationEligibilityEpoch;
  }

  public EpochNumber getActivationEpoch() {
    return activationEpoch;
  }

  public EpochNumber getExitEpoch() {
    return exitEpoch;
  }

  public EpochNumber getWithdrawableEpoch() {
    return withdrawableEpoch;
  }

  public Boolean getSlashed() {
    return slashed;
  }

  public Gwei getEffectiveBalance() {
    return effectiveBalance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValidatorRecord that = (ValidatorRecord) o;
    return Objects.equal(pubKey, that.pubKey)
        && Objects.equal(withdrawalCredentials, that.withdrawalCredentials)
        && Objects.equal(activationEpoch, that.activationEpoch)
        && Objects.equal(activationEligibilityEpoch, that.activationEligibilityEpoch)
        && Objects.equal(exitEpoch, that.exitEpoch)
        && Objects.equal(withdrawableEpoch, that.withdrawableEpoch)
        && Objects.equal(slashed, that.slashed)
        && Objects.equal(effectiveBalance, that.effectiveBalance);
  }

  public Builder builder() {
    return Builder.fromRecord(this);
  }

  @Override
  public String toString() {
    return "ValidatorRecord{"
        + "pubKey="
        + pubKey
        + ", withdrawalCredentials="
        + withdrawalCredentials
        + ", activationEpoch="
        + activationEpoch
        + ", activationEligibilityEpoch="
        + activationEligibilityEpoch
        + ", exitEpoch="
        + exitEpoch
        + ", withdrawableEpoch="
        + withdrawableEpoch
        + ", slashed="
        + slashed
        + ", effectiveBalance="
        + effectiveBalance
        + '}';
  }

  public static class Builder {

    private BLSPubkey pubKey;
    private Hash32 withdrawalCredentials;
    private EpochNumber activationEpoch;
    private EpochNumber activationEligibilityEpoch;
    private EpochNumber exitEpoch;
    private EpochNumber withdrawableEpoch;
    private Boolean slashed;
    private Gwei effectiveBalance;

    private Builder() {}

    public static Builder createEmpty() {
      return new Builder();
    }

    public static Builder fromDepositData(DepositData data) {
      Builder builder = new Builder();

      builder.pubKey = data.getPubKey();
      builder.withdrawalCredentials = data.getWithdrawalCredentials();

      return builder;
    }

    public static Builder fromRecord(ValidatorRecord record) {
      Builder builder = new Builder();

      builder.pubKey = record.pubKey;
      builder.withdrawalCredentials = record.withdrawalCredentials;
      builder.activationEpoch = record.activationEpoch;
      builder.activationEligibilityEpoch = record.activationEligibilityEpoch;
      builder.exitEpoch = record.exitEpoch;
      builder.withdrawableEpoch = record.withdrawableEpoch;
      builder.slashed = record.slashed;
      builder.effectiveBalance = record.effectiveBalance;

      return builder;
    }

    public ValidatorRecord build() {
      assert pubKey != null;
      assert withdrawalCredentials != null;
      assert activationEpoch != null;
      assert activationEligibilityEpoch != null;
      assert exitEpoch != null;
      assert withdrawableEpoch != null;
      assert slashed != null;
      assert effectiveBalance != null;

      return new ValidatorRecord(
          pubKey,
          withdrawalCredentials,
          activationEpoch,
          activationEligibilityEpoch,
          exitEpoch,
          withdrawableEpoch,
          slashed,
          effectiveBalance);
    }

    public Builder withPubKey(BLSPubkey pubKey) {
      this.pubKey = pubKey;
      return this;
    }

    public Builder withWithdrawalCredentials(Hash32 withdrawalCredentials) {
      this.withdrawalCredentials = withdrawalCredentials;
      return this;
    }

    public Builder withActivationEpoch(EpochNumber activationEpoch) {
      this.activationEpoch = activationEpoch;
      return this;
    }

    public Builder withActivationEligibilityEpoch(EpochNumber activationEligibilityEpoch) {
      this.activationEligibilityEpoch = activationEligibilityEpoch;
      return this;
    }

    public Builder withExitEpoch(EpochNumber exitEpoch) {
      this.exitEpoch = exitEpoch;
      return this;
    }

    public Builder withWithdrawableEpoch(EpochNumber withdrawableEpoch) {
      this.withdrawableEpoch = withdrawableEpoch;
      return this;
    }

    public Builder withSlashed(Boolean slashed) {
      this.slashed = slashed;
      return this;
    }

    public Builder withEffectiveBalance(Gwei effectiveBalance) {
      this.effectiveBalance = effectiveBalance;
      return this;
    }
  }
}

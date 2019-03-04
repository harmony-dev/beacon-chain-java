package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * A record denoting a validator in the validator registry.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#validatorrecord">ValidatorRecord
 *     in the spec</a>
 */
@SSZSerializable
public class ValidatorRecord {

  /** BLS public key. */
  @SSZ private final BLSPubkey pubKey;
  /** Withdrawal credentials. */
  @SSZ private final Hash32 withdrawalCredentials;
  /** Slot when validator activated */
  @SSZ private final EpochNumber activationEpoch;
  /** Slot when validator exited */
  @SSZ private final EpochNumber exitEpoch;
  /** Epoch when validator is eligible to withdraw */
  @SSZ private final EpochNumber withdrawableEpoch;
  /** Did the validator initiate an exit */
  @SSZ private final Boolean initiatedExit;
  /** Status flags. */
  @SSZ private final Boolean slashed;

  public ValidatorRecord(BLSPubkey pubKey,
      Hash32 withdrawalCredentials, EpochNumber activationEpoch,
      EpochNumber exitEpoch, EpochNumber withdrawableEpoch,
      Boolean initiatedExit, Boolean slashed) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.activationEpoch = activationEpoch;
    this.exitEpoch = exitEpoch;
    this.withdrawableEpoch = withdrawableEpoch;
    this.initiatedExit = initiatedExit;
    this.slashed = slashed;
  }

  public BLSPubkey getPubKey() {
    return pubKey;
  }

  public Hash32 getWithdrawalCredentials() {
    return withdrawalCredentials;
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

  public Boolean getInitiatedExit() {
    return initiatedExit;
  }

  public Boolean getSlashed() {
    return slashed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValidatorRecord that = (ValidatorRecord) o;
    return Objects.equal(pubKey, that.pubKey)
        && Objects.equal(withdrawalCredentials, that.withdrawalCredentials)
        && Objects.equal(activationEpoch, that.activationEpoch)
        && Objects.equal(exitEpoch, that.exitEpoch)
        && Objects.equal(withdrawableEpoch, that.withdrawableEpoch)
        && Objects.equal(initiatedExit, that.initiatedExit)
        && Objects.equal(slashed, that.slashed);
  }

  public Builder builder() {
    return Builder.fromRecord(this);
  }

  public static class Builder {

    private BLSPubkey pubKey;
    private Hash32 withdrawalCredentials;
    private EpochNumber activationEpoch;
    private EpochNumber exitEpoch;
    private EpochNumber withdrawableEpoch;
    private Boolean initiatedExit;
    private Boolean slashed;

    private Builder() {}

    public static Builder createEmpty() {
      return new Builder();
    }

    public static Builder fromDepositInput(DepositInput input) {
      Builder builder = new Builder();

      builder.pubKey = input.getPubKey();
      builder.withdrawalCredentials = input.getWithdrawalCredentials();

      return builder;
    }

    public static Builder fromRecord(ValidatorRecord record) {
      Builder builder = new Builder();

      builder.pubKey = record.pubKey;
      builder.withdrawalCredentials = record.withdrawalCredentials;
      builder.activationEpoch = record.activationEpoch;
      builder.exitEpoch = record.exitEpoch;
      builder.withdrawableEpoch = record.withdrawableEpoch;
      builder.initiatedExit = record.initiatedExit;
      builder.slashed = record.slashed;

      return builder;
    }

    public ValidatorRecord build() {
      assert pubKey != null;
      assert withdrawalCredentials != null;
      assert activationEpoch != null;
      assert exitEpoch != null;
      assert withdrawableEpoch != null;
      assert initiatedExit != null;
      assert slashed != null;

      return new ValidatorRecord(
          pubKey,
          withdrawalCredentials,
          activationEpoch,
          exitEpoch,
          withdrawableEpoch,
          initiatedExit,
          slashed);
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

    public Builder withExitEpoch(EpochNumber exitEpoch) {
      this.exitEpoch = exitEpoch;
      return this;
    }

    public Builder withWithdrawableEpoch(EpochNumber withdrawableEpoch) {
      this.withdrawableEpoch = withdrawableEpoch;
      return this;
    }

    public Builder withInitiatedExit(Boolean initiatedExit) {
      this.initiatedExit = initiatedExit;
      return this;
    }

    public Builder withSlashed(Boolean slashed) {
      this.slashed = slashed;
      return this;
    }
  }
}

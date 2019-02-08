package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.function.Function;

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
  /** Slot when validator withdrew */
  @SSZ private final EpochNumber withdrawalEpoch;
  /** Slot when validator was penalized */
  @SSZ private final EpochNumber penalizedEpoch;
  /** Status flags. */
  @SSZ private final UInt64 statusFlags;

  public ValidatorRecord(BLSPubkey pubKey,
      Hash32 withdrawalCredentials, EpochNumber activationEpoch,
      EpochNumber exitEpoch, EpochNumber withdrawalEpoch,
      EpochNumber penalizedEpoch, UInt64 statusFlags) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.activationEpoch = activationEpoch;
    this.exitEpoch = exitEpoch;
    this.withdrawalEpoch = withdrawalEpoch;
    this.penalizedEpoch = penalizedEpoch;
    this.statusFlags = statusFlags;
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

  public EpochNumber getWithdrawalEpoch() {
    return withdrawalEpoch;
  }

  public EpochNumber getPenalizedEpoch() {
    return penalizedEpoch;
  }

  public UInt64 getStatusFlags() {
    return statusFlags;
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
        && Objects.equal(withdrawalEpoch, that.withdrawalEpoch)
        && Objects.equal(penalizedEpoch, that.penalizedEpoch)
        && Objects.equal(statusFlags, that.statusFlags);
  }

  public Builder builder() {
    return Builder.fromRecord(this);
  }

  public static class Builder {

    private BLSPubkey pubKey;
    private Hash32 withdrawalCredentials;
    private EpochNumber activationEpoch;
    private EpochNumber exitEpoch;
    private EpochNumber withdrawalEpoch;
    private EpochNumber penalizedEpoch;
    private UInt64 statusFlags;

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
      builder.withdrawalEpoch = record.withdrawalEpoch;
      builder.penalizedEpoch = record.penalizedEpoch;
      builder.statusFlags = record.statusFlags;

      return builder;
    }

    public ValidatorRecord build() {
      assert pubKey != null;
      assert withdrawalCredentials != null;
      assert activationEpoch != null;
      assert exitEpoch != null;
      assert withdrawalEpoch != null;
      assert penalizedEpoch != null;
      assert statusFlags != null;

      return new ValidatorRecord(
          pubKey,
          withdrawalCredentials,
          activationEpoch,
          exitEpoch,
          withdrawalEpoch,
          penalizedEpoch,
          statusFlags);
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

    public Builder withWithdrawalEpoch(EpochNumber withdrawalEpoch) {
      this.withdrawalEpoch = withdrawalEpoch;
      return this;
    }

    public Builder withPenalizedEpoch(EpochNumber penalizedEpoch) {
      this.penalizedEpoch = penalizedEpoch;
      return this;
    }

    public Builder withStatusFlags(UInt64 statusFlags) {
      this.statusFlags = statusFlags;
      return this;
    }

    public Builder withStatusFlags(Function<UInt64, UInt64> statusFlagsUpdater) {
      this.statusFlags = statusFlagsUpdater.apply(statusFlags);
      return this;
    }
  }
}

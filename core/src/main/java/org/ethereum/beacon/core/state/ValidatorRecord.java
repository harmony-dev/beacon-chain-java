package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
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
  @SSZ private final Bytes48 pubKey;
  /** Withdrawal credentials. */
  @SSZ private final Hash32 withdrawalCredentials;
  /** Number of proposer slots since genesis. */
  @SSZ private final UInt64 proposerSlots;
  /** Slot when validator activated */
  @SSZ private final UInt64 activationSlot;
  /** Slot when validator exited */
  @SSZ private final UInt64 exitSlot;
  /** Slot when validator withdrew */
  @SSZ private final UInt64 withdrawalSlot;
  /** Slot when validator was penalized */
  @SSZ private final UInt64 penalizedSlot;
  /** Exit counter when validator exited (or 0). */
  @SSZ private final UInt64 exitCount;
  /** Status flags. */
  @SSZ private final UInt64 statusFlags;
  /** Slot the proof of custody seed was last changed. */
  @SSZ private final UInt64 latestCustodyReseedSlot;

  @SSZ private final UInt64 penultimateCustodyReseedSlot;

  public ValidatorRecord(
      Bytes48 pubKey,
      Hash32 withdrawalCredentials,
      UInt64 proposerSlots,
      UInt64 activationSlot,
      UInt64 exitSlot,
      UInt64 withdrawalSlot,
      UInt64 penalizedSlot,
      UInt64 exitCount,
      UInt64 statusFlags,
      UInt64 latestCustodyReseedSlot,
      UInt64 penultimateCustodyReseedSlot) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.proposerSlots = proposerSlots;
    this.activationSlot = activationSlot;
    this.exitSlot = exitSlot;
    this.withdrawalSlot = withdrawalSlot;
    this.penalizedSlot = penalizedSlot;
    this.exitCount = exitCount;
    this.statusFlags = statusFlags;
    this.latestCustodyReseedSlot = latestCustodyReseedSlot;
    this.penultimateCustodyReseedSlot = penultimateCustodyReseedSlot;
  }

  public Bytes48 getPubKey() {
    return pubKey;
  }

  public Hash32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  public UInt64 getProposerSlots() {
    return proposerSlots;
  }

  public UInt64 getActivationSlot() {
    return activationSlot;
  }

  public UInt64 getExitSlot() {
    return exitSlot;
  }

  public UInt64 getWithdrawalSlot() {
    return withdrawalSlot;
  }

  public UInt64 getPenalizedSlot() {
    return penalizedSlot;
  }

  public UInt64 getExitCount() {
    return exitCount;
  }

  public UInt64 getStatusFlags() {
    return statusFlags;
  }

  public UInt64 getLatestCustodyReseedSlot() {
    return latestCustodyReseedSlot;
  }

  public UInt64 getPenultimateCustodyReseedSlot() {
    return penultimateCustodyReseedSlot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValidatorRecord that = (ValidatorRecord) o;
    return Objects.equal(pubKey, that.pubKey)
        && Objects.equal(withdrawalCredentials, that.withdrawalCredentials)
        && Objects.equal(proposerSlots, that.proposerSlots)
        && Objects.equal(activationSlot, that.activationSlot)
        && Objects.equal(exitSlot, that.exitSlot)
        && Objects.equal(withdrawalSlot, that.withdrawalSlot)
        && Objects.equal(penalizedSlot, that.penalizedSlot)
        && Objects.equal(exitCount, that.exitCount)
        && Objects.equal(statusFlags, that.statusFlags)
        && Objects.equal(latestCustodyReseedSlot, that.latestCustodyReseedSlot)
        && Objects.equal(penultimateCustodyReseedSlot, that.penultimateCustodyReseedSlot);
  }

  public static class Builder {

    private Bytes48 pubKey;
    private Hash32 withdrawalCredentials;
    private UInt64 proposerSlots;
    private UInt64 activationSlot;
    private UInt64 exitSlot;
    private UInt64 withdrawalSlot;
    private UInt64 penalizedSlot;
    private UInt64 exitCount;
    private UInt64 statusFlags;
    private UInt64 latestCustodyReseedSlot;
    private UInt64 penultimateCustodyReseedSlot;

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
      builder.proposerSlots = record.proposerSlots;
      builder.activationSlot = record.activationSlot;
      builder.exitSlot = record.exitSlot;
      builder.withdrawalSlot = record.withdrawalSlot;
      builder.penalizedSlot = record.penalizedSlot;
      builder.exitCount = record.exitCount;
      builder.statusFlags = record.statusFlags;
      builder.latestCustodyReseedSlot = record.latestCustodyReseedSlot;
      builder.penultimateCustodyReseedSlot = record.penultimateCustodyReseedSlot;

      return builder;
    }

    public ValidatorRecord build() {
      assert pubKey != null;
      assert withdrawalCredentials != null;
      assert proposerSlots != null;
      assert activationSlot != null;
      assert exitSlot != null;
      assert withdrawalSlot != null;
      assert penalizedSlot != null;
      assert exitCount != null;
      assert statusFlags != null;
      assert latestCustodyReseedSlot != null;
      assert penultimateCustodyReseedSlot != null;

      return new ValidatorRecord(
          pubKey,
          withdrawalCredentials,
          proposerSlots,
          activationSlot,
          exitSlot,
          withdrawalSlot,
          penalizedSlot,
          exitCount,
          statusFlags,
          latestCustodyReseedSlot,
          penultimateCustodyReseedSlot);
    }

    public Builder withPubKey(Bytes48 pubKey) {
      this.pubKey = pubKey;
      return this;
    }

    public Builder withWithdrawalCredentials(Hash32 withdrawalCredentials) {
      this.withdrawalCredentials = withdrawalCredentials;
      return this;
    }

    public Builder withProposerSlots(UInt64 proposerSlots) {
      this.proposerSlots = proposerSlots;
      return this;
    }

    public Builder withActivationSlot(UInt64 activationSlot) {
      this.activationSlot = activationSlot;
      return this;
    }

    public Builder withExitSlot(UInt64 exitSlot) {
      this.exitSlot = exitSlot;
      return this;
    }

    public Builder withWithdrawalSlot(UInt64 withdrawalSlot) {
      this.withdrawalSlot = withdrawalSlot;
      return this;
    }

    public Builder withPenalizedSlot(UInt64 penalizedSlot) {
      this.penalizedSlot = penalizedSlot;
      return this;
    }

    public Builder withExitCount(UInt64 exitCount) {
      this.exitCount = exitCount;
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

    public Builder withLatestCustodyReseedSlot(UInt64 latestCustodyReseedSlot) {
      this.latestCustodyReseedSlot = latestCustodyReseedSlot;
      return this;
    }

    public Builder withPenultimateCustodyReseedSlot(UInt64 penultimateCustodyReseedSlot) {
      this.penultimateCustodyReseedSlot = penultimateCustodyReseedSlot;
      return this;
    }
  }
}

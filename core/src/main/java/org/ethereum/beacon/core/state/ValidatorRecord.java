package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.types.BLSPubkey;
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
  /** Number of proposer slots since genesis. */
  @SSZ private final SlotNumber proposerSlots;
  /** Slot when validator activated */
  @SSZ private final SlotNumber activationSlot;
  /** Slot when validator exited */
  @SSZ private final SlotNumber exitSlot;
  /** Slot when validator withdrew */
  @SSZ private final SlotNumber withdrawalSlot;
  /** Slot when validator was penalized */
  @SSZ private final SlotNumber penalizedSlot;
  /** Exit counter when validator exited (or 0). */
  @SSZ private final UInt64 exitCount;
  /** Status flags. */
  @SSZ private final UInt64 statusFlags;
  /** Slot the proof of custody seed was last changed. */
  @SSZ private final UInt64 latestCustodyReseedSlot;

  @SSZ private final UInt64 penultimateCustodyReseedSlot;

  public ValidatorRecord(
      BLSPubkey pubKey,
      Hash32 withdrawalCredentials,
      SlotNumber proposerSlots,
      SlotNumber activationSlot,
      SlotNumber exitSlot,
      SlotNumber withdrawalSlot,
      SlotNumber penalizedSlot,
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

  public BLSPubkey getPubKey() {
    return pubKey;
  }

  public Hash32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  public SlotNumber getProposerSlots() {
    return proposerSlots;
  }

  public SlotNumber getActivationSlot() {
    return activationSlot;
  }

  public SlotNumber getExitSlot() {
    return exitSlot;
  }

  public SlotNumber getWithdrawalSlot() {
    return withdrawalSlot;
  }

  public SlotNumber getPenalizedSlot() {
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

  public Builder builder() {
    return Builder.fromRecord(this);
  }

  public static class Builder {

    private BLSPubkey pubKey;
    private Hash32 withdrawalCredentials;
    private SlotNumber proposerSlots;
    private SlotNumber activationSlot;
    private SlotNumber exitSlot;
    private SlotNumber withdrawalSlot;
    private SlotNumber penalizedSlot;
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

    public Builder withPubKey(BLSPubkey pubKey) {
      this.pubKey = pubKey;
      return this;
    }

    public Builder withWithdrawalCredentials(Hash32 withdrawalCredentials) {
      this.withdrawalCredentials = withdrawalCredentials;
      return this;
    }

    public Builder withProposerSlots(SlotNumber proposerSlots) {
      this.proposerSlots = proposerSlots;
      return this;
    }

    public Builder withActivationSlot(SlotNumber activationSlot) {
      this.activationSlot = activationSlot;
      return this;
    }

    public Builder withExitSlot(SlotNumber exitSlot) {
      this.exitSlot = exitSlot;
      return this;
    }

    public Builder withWithdrawalSlot(SlotNumber withdrawalSlot) {
      this.withdrawalSlot = withdrawalSlot;
      return this;
    }

    public Builder withPenalizedSlot(SlotNumber penalizedSlot) {
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

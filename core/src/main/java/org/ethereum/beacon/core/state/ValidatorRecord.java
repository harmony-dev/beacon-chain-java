package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt64;

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
  @SSZ
  private final Bytes48 pubKey;
  /** Withdrawal credentials. */
  @SSZ
  private final Hash32 withdrawalCredentials;
  /** RANDAO commitment. */
  @SSZ
  private final Hash32 randaoCommitment;
  /** Slots the proposer has skipped (i.e. layers of RANDAO expected). */
  @SSZ
  private final UInt64 randaoLayers;
  /** Slot when validator activated */
  @SSZ
  private final UInt64 activationSlot;
  /** Slot when validator exited */
  @SSZ
  private final UInt64 exitSlot;
  /** Slot when validator withdrew */
  @SSZ
  private final UInt64 withdrawalSlot;
  /** Slot when validator was penalized */
  @SSZ
  private final UInt64 penalizedSlot;
  /** Exit counter when validator exited (or 0). */
  @SSZ
  private final UInt64 exitCount;
  /** Status flags. */
  @SSZ
  private final UInt64 statusFlags;
  /** Proof of custody commitment. */
  @SSZ
  private final Hash32 custodyCommitment;
  /** Slot the proof of custody seed was last changed. */
  @SSZ
  private final UInt64 latestCustodyReseedSlot;

  @SSZ
  private final UInt64 penultimateCustodyReseedSlot;

  public ValidatorRecord(
      Bytes48 pubKey,
      Hash32 withdrawalCredentials,
      Hash32 randaoCommitment,
      UInt64 randaoLayers,
      UInt64 activationSlot,
      UInt64 exitSlot,
      UInt64 withdrawalSlot,
      UInt64 penalizedSlot,
      UInt64 exitCount,
      UInt64 statusFlags,
      Hash32 custodyCommitment,
      UInt64 latestCustodyReseedSlot,
      UInt64 penultimateCustodyReseedSlot) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.randaoCommitment = randaoCommitment;
    this.randaoLayers = randaoLayers;
    this.activationSlot = activationSlot;
    this.exitSlot = exitSlot;
    this.withdrawalSlot = withdrawalSlot;
    this.penalizedSlot = penalizedSlot;
    this.exitCount = exitCount;
    this.statusFlags = statusFlags;
    this.custodyCommitment = custodyCommitment;
    this.latestCustodyReseedSlot = latestCustodyReseedSlot;
    this.penultimateCustodyReseedSlot = penultimateCustodyReseedSlot;
  }

  public Bytes48 getPubKey() {
    return pubKey;
  }

  public Hash32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  public Hash32 getRandaoCommitment() {
    return randaoCommitment;
  }

  public UInt64 getRandaoLayers() {
    return randaoLayers;
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

  public Hash32 getCustodyCommitment() {
    return custodyCommitment;
  }

  public UInt64 getLatestCustodyReseedSlot() {
    return latestCustodyReseedSlot;
  }

  public UInt64 getPenultimateCustodyReseedSlot() {
    return penultimateCustodyReseedSlot;
  }

  public static class Builder {

    private Bytes48 pubKey;
    private Hash32 withdrawalCredentials;
    private Hash32 randaoCommitment;
    private UInt64 randaoLayers;
    private UInt64 activationSlot;
    private UInt64 exitSlot;
    private UInt64 withdrawalSlot;
    private UInt64 penalizedSlot;
    private UInt64 exitCount;
    private UInt64 statusFlags;
    private Hash32 custodyCommitment;
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
      builder.randaoCommitment = input.getRandaoCommitment();
      builder.custodyCommitment = input.getCustodyCommitment();

      return builder;
    }

    public static Builder fromRecord(ValidatorRecord record) {
      Builder builder = new Builder();

      builder.pubKey = record.pubKey;
      builder.withdrawalCredentials = record.withdrawalCredentials;
      builder.randaoCommitment = record.randaoCommitment;
      builder.randaoLayers = record.randaoLayers;
      builder.activationSlot = record.activationSlot;
      builder.exitSlot = record.exitSlot;
      builder.withdrawalSlot = record.withdrawalSlot;
      builder.penalizedSlot = record.penalizedSlot;
      builder.exitCount = record.exitCount;
      builder.statusFlags = record.statusFlags;
      builder.custodyCommitment = record.custodyCommitment;
      builder.latestCustodyReseedSlot = record.latestCustodyReseedSlot;
      builder.penultimateCustodyReseedSlot = record.penultimateCustodyReseedSlot;

      return builder;
    }

    public ValidatorRecord build() {
      assert pubKey != null;
      assert withdrawalCredentials != null;
      assert randaoCommitment != null;
      assert randaoLayers != null;
      assert activationSlot != null;
      assert exitSlot != null;
      assert withdrawalSlot != null;
      assert penalizedSlot != null;
      assert exitCount != null;
      assert statusFlags != null;
      assert custodyCommitment != null;
      assert latestCustodyReseedSlot != null;
      assert penultimateCustodyReseedSlot != null;

      return new ValidatorRecord(
          pubKey,
          withdrawalCredentials,
          randaoCommitment,
          randaoLayers,
          activationSlot,
          exitSlot,
          withdrawalSlot,
          penalizedSlot,
          exitCount,
          statusFlags,
          custodyCommitment,
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

    public Builder withRandaoCommitment(Hash32 randaoCommitment) {
      this.randaoCommitment = randaoCommitment;
      return this;
    }

    public Builder withRandaoLayers(UInt64 randaoLayers) {
      this.randaoLayers = randaoLayers;
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

    public Builder withCustodyCommitment(Hash32 custodyCommitment) {
      this.custodyCommitment = custodyCommitment;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValidatorRecord that = (ValidatorRecord) o;
    return pubKey.equals(that.pubKey) &&
        withdrawalCredentials.equals(that.withdrawalCredentials) &&
        randaoCommitment.equals(that.randaoCommitment) &&
        randaoLayers.equals(that.randaoLayers) &&
        activationSlot.equals(that.activationSlot) &&
        exitSlot.equals(that.exitSlot) &&
        withdrawalSlot.equals(that.withdrawalSlot) &&
        penalizedSlot.equals(that.penalizedSlot) &&
        exitCount.equals(that.exitCount) &&
        statusFlags.equals(that.statusFlags) &&
        custodyCommitment.equals(that.custodyCommitment) &&
        latestCustodyReseedSlot.equals(that.latestCustodyReseedSlot) &&
        penultimateCustodyReseedSlot.equals(that.penultimateCustodyReseedSlot);
  }
}

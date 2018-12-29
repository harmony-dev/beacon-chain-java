package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
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
public class ValidatorRecord {

  /**
   * TTL of validator which balance is equal to 0.
   *
   * <p>Set to 4,194,304 slots which is about 291 days
   */
  public static final UInt64 ZERO_BALANCE_VALIDATOR_TTL = UInt64.valueOf(1 << 22);

  /** BLS public key. */
  private final Bytes48 pubKey;
  /** Withdrawal credentials. */
  private final Hash32 withdrawalCredentials;
  /** RANDAO commitment. */
  private final Hash32 randaoCommitment;
  /** Slots the proposer has skipped (i.e. layers of RANDAO expected). */
  private final UInt64 randaoLayers;
  /** Status code. */
  private final UInt64 status;
  /** Slot when validator last changed status (or 0). */
  private final UInt64 latestStatusChangeSlot;
  /** Exit counter when validator exited (or 0). */
  private final UInt64 exitCount;
  /** Proof of custody commitment. */
  private final Hash32 pocCommitment;
  /** Slot the proof of custody seed was last changed. */
  private final UInt64 lastPocChangeSlot;

  private final UInt64 secondLastPocChangeSlot;

  public ValidatorRecord(
      Bytes48 pubKey,
      Hash32 withdrawalCredentials,
      Hash32 randaoCommitment,
      UInt64 randaoLayers,
      UInt64 status,
      UInt64 latestStatusChangeSlot,
      UInt64 exitCount,
      Hash32 pocCommitment,
      UInt64 lastPocChangeSlot,
      UInt64 secondLastPocChangeSlot) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.randaoCommitment = randaoCommitment;
    this.randaoLayers = randaoLayers;
    this.status = status;
    this.latestStatusChangeSlot = latestStatusChangeSlot;
    this.exitCount = exitCount;
    this.pocCommitment = pocCommitment;
    this.lastPocChangeSlot = lastPocChangeSlot;
    this.secondLastPocChangeSlot = secondLastPocChangeSlot;
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

  public ValidatorStatus getStatus() {
    return ValidatorStatus.valueOf(status);
  }

  public UInt64 getLatestStatusChangeSlot() {
    return latestStatusChangeSlot;
  }

  public UInt64 getExitCount() {
    return exitCount;
  }

  public Hash32 getPocCommitment() {
    return pocCommitment;
  }

  public UInt64 getLastPocChangeSlot() {
    return lastPocChangeSlot;
  }

  public UInt64 getSecondLastPocChangeSlot() {
    return secondLastPocChangeSlot;
  }

  public static class Builder {

    private Bytes48 pubKey;
    private Hash32 withdrawalCredentials;
    private Hash32 randaoCommitment;
    private UInt64 randaoLayers;
    private UInt64 status;
    private UInt64 latestStatusChangeSlot;
    private UInt64 exitCount;
    private Hash32 pocCommitment;
    private UInt64 lastPocChangeSlot;
    private UInt64 secondLastPocChangeSlot;

    private Builder() {}

    public static Builder createEmpty() {
      return new Builder();
    }

    public static Builder fromDepositInput(DepositInput input) {
      Builder builder = new Builder();

      builder.pubKey = input.getPubKey();
      builder.withdrawalCredentials = input.getWithdrawalCredentials();
      builder.randaoCommitment = input.getRandaoCommitment();
      builder.pocCommitment = input.getPocCommitment();

      return builder;
    }

    public static Builder fromRecord(ValidatorRecord record) {
      Builder builder = new Builder();

      builder.pubKey = record.pubKey;
      builder.withdrawalCredentials = record.withdrawalCredentials;
      builder.randaoCommitment = record.randaoCommitment;
      builder.randaoLayers = record.randaoLayers;
      builder.status = record.status;
      builder.latestStatusChangeSlot = record.latestStatusChangeSlot;
      builder.exitCount = record.exitCount;
      builder.pocCommitment = record.pocCommitment;
      builder.lastPocChangeSlot = record.lastPocChangeSlot;
      builder.secondLastPocChangeSlot = record.secondLastPocChangeSlot;

      return builder;
    }

    public ValidatorRecord build() {
      assert pubKey != null;
      assert withdrawalCredentials != null;
      assert randaoCommitment != null;
      assert randaoLayers != null;
      assert status != null;
      assert latestStatusChangeSlot != null;
      assert exitCount != null;
      assert pocCommitment != null;
      assert lastPocChangeSlot != null;
      assert secondLastPocChangeSlot != null;

      return new ValidatorRecord(
          pubKey,
          withdrawalCredentials,
          randaoCommitment,
          randaoLayers,
          status,
          latestStatusChangeSlot,
          exitCount,
          pocCommitment,
          lastPocChangeSlot,
          secondLastPocChangeSlot);
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

    public Builder withStatus(ValidatorStatus status) {
      this.status = status.getCode();
      return this;
    }

    public Builder withLatestStatusChangeSlot(UInt64 latestStatusChangeSlot) {
      this.latestStatusChangeSlot = latestStatusChangeSlot;
      return this;
    }

    public Builder withExitCount(UInt64 exitCount) {
      this.exitCount = exitCount;
      return this;
    }

    public Builder withPocCommitment(Hash32 pocCommitment) {
      this.pocCommitment = pocCommitment;
      return this;
    }

    public Builder withLastPocChangeSlot(UInt64 lastPocChangeSlot) {
      this.lastPocChangeSlot = lastPocChangeSlot;
      return this;
    }

    public Builder withSecondLastPocChangeSlot(UInt64 secondLastPocChangeSlot) {
      this.secondLastPocChangeSlot = secondLastPocChangeSlot;
      return this;
    }
  }
}

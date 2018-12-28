package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
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

  public UInt64 getStatus() {
    return status;
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
}

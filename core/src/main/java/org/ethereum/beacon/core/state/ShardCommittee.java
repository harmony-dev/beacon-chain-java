package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Validator committee assigned to a certain shard.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#shardcommittee">ShardCommitee
 *     in the spec</a>
 */
public class ShardCommittee {

  /** Shard number. */
  private final UInt64 shard;
  /** Validator indices. */
  private final UInt24[] committee;
  /** Total validator count (for proofs of custody). */
  private final UInt64 totalValidatorCount;

  public ShardCommittee(UInt64 shard, UInt24[] committee, UInt64 totalValidatorCount) {
    this.shard = shard;
    this.committee = committee;
    this.totalValidatorCount = totalValidatorCount;
  }

  public UInt64 getShard() {
    return shard;
  }

  public UInt24[] getCommittee() {
    return committee;
  }

  public UInt64 getTotalValidatorCount() {
    return totalValidatorCount;
  }
}

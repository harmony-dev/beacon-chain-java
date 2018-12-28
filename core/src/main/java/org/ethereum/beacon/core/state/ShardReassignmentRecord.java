package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Record denoting validator's reassignment to a certain shard.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#shardreassignmentrecord">ShardReassignmentRecord
 *     in the spec</a>
 */
public class ShardReassignmentRecord {

  /** Which validator to reassign. */
  private final UInt24 validatorIndex;
  /** To which shard. */
  private final UInt64 shard;
  /** When. */
  private final UInt64 slot;

  public ShardReassignmentRecord(UInt24 validatorIndex, UInt64 shard, UInt64 slot) {
    this.validatorIndex = validatorIndex;
    this.shard = shard;
    this.slot = slot;
  }

  public UInt24 getValidatorIndex() {
    return validatorIndex;
  }

  public UInt64 getShard() {
    return shard;
  }

  public UInt64 getSlot() {
    return slot;
  }
}

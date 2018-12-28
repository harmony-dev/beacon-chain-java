package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Crosslink to a shard block.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#crosslinkrecord">CrosslinkRecord
 *     in the spec</a>
 */
public class CrosslinkRecord {

  /** Slot number. */
  private final UInt64 slot;
  /** Shard block hash. */
  private final Hash32 shardBlockRoot;

  public CrosslinkRecord(UInt64 slot, Hash32 shardBlockRoot) {
    this.slot = slot;
    this.shardBlockRoot = shardBlockRoot;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public Hash32 getShardBlockRoot() {
    return shardBlockRoot;
  }
}

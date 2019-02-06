package org.ethereum.beacon.core.operations.attestation;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Attestation data that validators are signing off on.
 *
 * @see Attestation
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#attestationdata">AttestationData
 *     in the spec</a>
 */
@SSZSerializable
public class AttestationData {

  /** Slot number. */
  @SSZ private final SlotNumber slot;
  /** Shard number. */
  @SSZ private final ShardNumber shard;
  /** Hash of signed beacon block. */
  @SSZ private final Hash32 beaconBlockRoot;
  /** Hash of beacon block's ancestor at the epoch boundary. */
  @SSZ private final Hash32 epochBoundaryRoot;
  /** Hash of shard's block. */
  @SSZ private final Hash32 shardBlockRoot;
  /** Hash of last crosslink block. */
  @SSZ private final Hash32 latestCrosslinkRoot;
  /** Slot of the last justified beacon block. */
  @SSZ private final SlotNumber justifiedSlot;
  /** Hash of the last justified beacon block. */
  @SSZ private final Hash32 justifiedBlockRoot;

  public AttestationData(
      SlotNumber slot,
      ShardNumber shard,
      Hash32 beaconBlockRoot,
      Hash32 epochBoundaryRoot,
      Hash32 shardBlockRoot,
      Hash32 latestCrosslinkRoot,
      SlotNumber justifiedSlot,
      Hash32 justifiedBlockRoot) {
    this.slot = slot;
    this.shard = shard;
    this.beaconBlockRoot = beaconBlockRoot;
    this.epochBoundaryRoot = epochBoundaryRoot;
    this.shardBlockRoot = shardBlockRoot;
    this.latestCrosslinkRoot = latestCrosslinkRoot;
    this.justifiedSlot = justifiedSlot;
    this.justifiedBlockRoot = justifiedBlockRoot;
  }

  public SlotNumber getSlot() {
    return slot;
  }

  public ShardNumber getShard() {
    return shard;
  }

  public Hash32 getBeaconBlockRoot() {
    return beaconBlockRoot;
  }

  public Hash32 getEpochBoundaryRoot() {
    return epochBoundaryRoot;
  }

  public Hash32 getShardBlockRoot() {
    return shardBlockRoot;
  }

  public Hash32 getLatestCrosslinkRoot() {
    return latestCrosslinkRoot;
  }

  public SlotNumber getJustifiedSlot() {
    return justifiedSlot;
  }

  public Hash32 getJustifiedBlockRoot() {
    return justifiedBlockRoot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AttestationData that = (AttestationData) o;
    return Objects.equal(slot, that.slot)
        && Objects.equal(shard, that.shard)
        && Objects.equal(beaconBlockRoot, that.beaconBlockRoot)
        && Objects.equal(epochBoundaryRoot, that.epochBoundaryRoot)
        && Objects.equal(shardBlockRoot, that.shardBlockRoot)
        && Objects.equal(latestCrosslinkRoot, that.latestCrosslinkRoot)
        && Objects.equal(justifiedSlot, that.justifiedSlot)
        && Objects.equal(justifiedBlockRoot, that.justifiedBlockRoot);
  }
}

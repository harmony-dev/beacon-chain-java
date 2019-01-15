package org.ethereum.beacon.core.operations.attestation;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.core.operations.Attestation;
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
  @SSZ
  private final UInt64 slot;
  /** Shard number. */
  @SSZ
  private final UInt64 shard;
  /** Hash of signed beacon block. */
  @SSZ
  private final Hash32 beaconBlockRoot;
  /** Hash of beacon block's ancestor at the epoch boundary. */
  @SSZ
  private final Hash32 epochBoundaryRoot;
  /** Hash of shard's block. */
  @SSZ
  private final Hash32 shardBlockRoot;
  /** Hash of last crosslink block. */
  @SSZ
  private final Hash32 latestCrosslinkRoot;
  /** Slot of the last justified beacon block. */
  @SSZ
  private final UInt64 justifiedSlot;
  /** Hash of the last justified beacon block. */
  @SSZ
  private final Hash32 justifiedBlockRoot;

  public AttestationData(
      UInt64 slot,
      UInt64 shard,
      Hash32 beaconBlockRoot,
      Hash32 epochBoundaryRoot,
      Hash32 shardBlockRoot,
      Hash32 latestCrosslinkRoot,
      UInt64 justifiedSlot,
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

  public UInt64 getSlot() {
    return slot;
  }

  public UInt64 getShard() {
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

  public UInt64 getJustifiedSlot() {
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
    return slot.equals(that.slot) &&
        shard.equals(that.shard) &&
        beaconBlockRoot.equals(that.beaconBlockRoot) &&
        epochBoundaryRoot.equals(that.epochBoundaryRoot) &&
        shardBlockRoot.equals(that.shardBlockRoot) &&
        latestCrosslinkRoot.equals(that.latestCrosslinkRoot) &&
        justifiedSlot.equals(that.justifiedSlot) &&
        justifiedBlockRoot.equals(that.justifiedBlockRoot);
  }
}

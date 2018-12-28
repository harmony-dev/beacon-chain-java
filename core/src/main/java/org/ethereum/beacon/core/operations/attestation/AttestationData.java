package org.ethereum.beacon.core.operations.attestation;

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
public class AttestationData {

  /** Slot number. */
  private final UInt64 slot;
  /** Shard number. */
  private final UInt64 shard;
  /** Hash of signed beacon block. */
  private final Hash32 beaconBlockRoot;
  /** Hash of beacon block's ancestor at the epoch boundary. */
  private final Hash32 epochBoundaryRoot;
  /** Hash of shard's block. */
  private final Hash32 shardBlockRoot;
  /** Hash of last crosslink block. */
  private final Hash32 latestCrosslinkRoot;
  /** Slot of the last justified beacon block. */
  private final UInt64 justifiedSlot;
  /** Hash of the last justified beacon block. */
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
}

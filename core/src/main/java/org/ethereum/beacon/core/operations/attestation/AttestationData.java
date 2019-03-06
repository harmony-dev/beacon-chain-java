package org.ethereum.beacon.core.operations.attestation;

import com.google.common.base.Objects;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

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
  /** Root of the signed beacon block. */
  @SSZ private final Hash32 beaconBlockRoot;
  /** Root of the ancestor at the epoch boundary. */
  @SSZ private final Hash32 epochBoundaryRoot;
  /** Data from the shard since the last attestation. */
  @SSZ private final Hash32 crosslinkDataRoot;
  /** Last crosslink. */
  @SSZ private final Crosslink latestCrosslink;
  /** Slot of the last justified beacon block. */
  @SSZ private final EpochNumber justifiedEpoch;
  /** Hash of the last justified beacon block. */
  @SSZ private final Hash32 justifiedBlockRoot;

  public AttestationData(
      SlotNumber slot,
      ShardNumber shard,
      Hash32 beaconBlockRoot,
      Hash32 epochBoundaryRoot,
      Hash32 crosslinkDataRoot,
      Crosslink latestCrosslink,
      EpochNumber justifiedEpoch,
      Hash32 justifiedBlockRoot) {
    this.slot = slot;
    this.shard = shard;
    this.beaconBlockRoot = beaconBlockRoot;
    this.epochBoundaryRoot = epochBoundaryRoot;
    this.crosslinkDataRoot = crosslinkDataRoot;
    this.latestCrosslink = latestCrosslink;
    this.justifiedEpoch = justifiedEpoch;
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

  public Hash32 getCrosslinkDataRoot() {
    return crosslinkDataRoot;
  }

  public Crosslink getLatestCrosslink() {
    return latestCrosslink;
  }

  public EpochNumber getJustifiedEpoch() {
    return justifiedEpoch;
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
        && Objects.equal(crosslinkDataRoot, that.crosslinkDataRoot)
        && Objects.equal(latestCrosslink, that.latestCrosslink)
        && Objects.equal(justifiedEpoch, that.justifiedEpoch)
        && Objects.equal(justifiedBlockRoot, that.justifiedBlockRoot);
  }

  @Override
  public int hashCode() {
    int result = slot != null ? slot.hashCode() : 0;
    result = 31 * result + (shard != null ? shard.hashCode() : 0);
    result = 31 * result + (beaconBlockRoot != null ? beaconBlockRoot.hashCode() : 0);
    result = 31 * result + (epochBoundaryRoot != null ? epochBoundaryRoot.hashCode() : 0);
    result = 31 * result + (crosslinkDataRoot != null ? crosslinkDataRoot.hashCode() : 0);
    result = 31 * result + (latestCrosslink != null ? latestCrosslink.hashCode() : 0);
    result = 31 * result + (justifiedEpoch != null ? justifiedEpoch.hashCode() : 0);
    result = 31 * result + (justifiedBlockRoot != null ? justifiedBlockRoot.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable SpecConstants spec,@Nullable Time beaconStart) {
    return "AttestationData[slot="
        + slot.toStringNumber(spec)
        + ", shard=" + shard.toString(spec)
        + ", beaconBlock=" + beaconBlockRoot.toStringShort()
        + ", epochBoundary=" + epochBoundaryRoot.toStringShort()
        + ", shardBlock=" + crosslinkDataRoot.toStringShort()
        + ", latestCrosslink=" + latestCrosslink.toString(spec)
        + ", justifiedEpoch=" + justifiedEpoch.toString(spec)
        + ", justifiedBlock=" + justifiedBlockRoot.toStringShort()
        +"]";
  }
}

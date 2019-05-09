package org.ethereum.beacon.core.operations.attestation;

import com.google.common.base.Objects;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Attestation data that validators are signing off on.
 *
 * @see Attestation
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#attestationdata">AttestationData
 *     in the spec</a>
 */
@SSZSerializable
public class AttestationData {

  // LMD GHOST vote:

  /** Root of the signed beacon block. */
  @SSZ private final Hash32 beaconBlockRoot;

  // FFG vote:

  /** FFG source epoch. */
  @SSZ private final EpochNumber sourceEpoch;
  /** FFG source block root. */
  @SSZ private final Hash32 sourceRoot;
  /** FFG target epoch. */
  @SSZ private final EpochNumber targetEpoch;
  /** FFG target block root. */
  @SSZ private final Hash32 targetRoot;

  // Crosslink vote:

  /** Shard number. */
  @SSZ private final ShardNumber shard;
  /** Previous crosslink root. */
  @SSZ private final Hash32 previousCrosslinkRoot;
  /** Data from the shard since the last attestation. */
  @SSZ private final Hash32 crosslinkDataRoot;

  public AttestationData(
      Hash32 beaconBlockRoot,
      EpochNumber sourceEpoch,
      Hash32 sourceRoot,
      EpochNumber targetEpoch,
      Hash32 targetRoot,
      ShardNumber shard,
      Hash32 previousCrosslinkRoot,
      Hash32 crosslinkDataRoot) {
    this.beaconBlockRoot = beaconBlockRoot;
    this.sourceEpoch = sourceEpoch;
    this.sourceRoot = sourceRoot;
    this.targetEpoch = targetEpoch;
    this.targetRoot = targetRoot;
    this.shard = shard;
    this.previousCrosslinkRoot = previousCrosslinkRoot;
    this.crosslinkDataRoot = crosslinkDataRoot;
  }

  public ShardNumber getShard() {
    return shard;
  }

  public Hash32 getBeaconBlockRoot() {
    return beaconBlockRoot;
  }

  public EpochNumber getTargetEpoch() {
    return targetEpoch;
  }

  public Hash32 getTargetRoot() {
    return targetRoot;
  }

  public Hash32 getCrosslinkDataRoot() {
    return crosslinkDataRoot;
  }

  public Hash32 getPreviousCrosslinkRoot() {
    return previousCrosslinkRoot;
  }

  public EpochNumber getSourceEpoch() {
    return sourceEpoch;
  }

  public Hash32 getSourceRoot() {
    return sourceRoot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AttestationData that = (AttestationData) o;
    return Objects.equal(shard, that.shard)
        && Objects.equal(beaconBlockRoot, that.beaconBlockRoot)
        && Objects.equal(targetRoot, that.targetRoot)
        && Objects.equal(crosslinkDataRoot, that.crosslinkDataRoot)
        && Objects.equal(previousCrosslinkRoot, that.previousCrosslinkRoot)
        && Objects.equal(sourceEpoch, that.sourceEpoch)
        && Objects.equal(targetEpoch, that.targetEpoch)
        && Objects.equal(sourceRoot, that.sourceRoot);
  }

  @Override
  public int hashCode() {
    int result = shard != null ? shard.hashCode() : 0;
    result = 31 * result + (beaconBlockRoot != null ? beaconBlockRoot.hashCode() : 0);
    result = 31 * result + (targetRoot != null ? targetRoot.hashCode() : 0);
    result = 31 * result + (crosslinkDataRoot != null ? crosslinkDataRoot.hashCode() : 0);
    result = 31 * result + (previousCrosslinkRoot != null ? previousCrosslinkRoot.hashCode() : 0);
    result = 31 * result + (sourceEpoch != null ? sourceEpoch.hashCode() : 0);
    result = 31 * result + (sourceRoot != null ? sourceRoot.hashCode() : 0);
    result = 31 * result + (targetEpoch != null ? targetEpoch.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable SpecConstants spec,@Nullable Time beaconStart) {
    return "AttestationData[shard="
        + shard.toString()
        + ", beaconBlock=" + beaconBlockRoot.toStringShort()
        + ", targetEpoch=" + targetEpoch.toString(spec)
        + ", targetRoot=" + targetRoot.toStringShort()
        + ", shardBlock=" + crosslinkDataRoot.toStringShort()
        + ", previousCrosslinkRoot=" + previousCrosslinkRoot.toStringShort()
        + ", sourceEpoch=" + sourceEpoch.toString(spec)
        + ", sourceRoot=" + sourceRoot.toStringShort()
        +"]";
  }
}

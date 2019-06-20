package org.ethereum.beacon.core.operations.attestation;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Attestation data that validators are signing off on.
 *
 * @see Attestation
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.0/specs/core/0_beacon-chain.md#attestationdata">AttestationData
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

  @SSZ private final Crosslink crosslink;

  public AttestationData(
      Hash32 beaconBlockRoot,
      EpochNumber sourceEpoch,
      Hash32 sourceRoot,
      EpochNumber targetEpoch,
      Hash32 targetRoot,
      Crosslink crosslink) {
    this.beaconBlockRoot = beaconBlockRoot;
    this.sourceEpoch = sourceEpoch;
    this.sourceRoot = sourceRoot;
    this.targetEpoch = targetEpoch;
    this.targetRoot = targetRoot;
    this.crosslink = crosslink;
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

  public EpochNumber getSourceEpoch() {
    return sourceEpoch;
  }

  public Hash32 getSourceRoot() {
    return sourceRoot;
  }

  public Crosslink getCrosslink() {
    return crosslink;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttestationData that = (AttestationData) o;
    return Objects.equal(beaconBlockRoot, that.beaconBlockRoot)
        && Objects.equal(sourceEpoch, that.sourceEpoch)
        && Objects.equal(sourceRoot, that.sourceRoot)
        && Objects.equal(targetEpoch, that.targetEpoch)
        && Objects.equal(targetRoot, that.targetRoot)
        && Objects.equal(crosslink, that.crosslink);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        beaconBlockRoot, sourceEpoch, sourceRoot, targetEpoch, targetRoot, crosslink);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("crosslink", crosslink)
        .add("beaconBlockRoot", beaconBlockRoot.toStringShort())
        .add("sourceEpoch", sourceEpoch)
        .add("sourceRoot", sourceRoot.toStringShort())
        .add("targetEpoch", targetEpoch)
        .add("targetRoot", targetRoot.toStringShort())
        .toString();
  }
}

package org.ethereum.beacon.core.operations.attestation;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Attestation data that validators are signing off on.
 *
 * @see Attestation
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.9.2/specs/core/0_beacon-chain.md#attestationdata">AttestationData
 *     in the spec</a>
 */
@SSZSerializable
public class AttestationData {

  @SSZ private final SlotNumber slot;
  @SSZ private final CommitteeIndex index;

  // LMD GHOST vote:

  /** Root of the signed beacon block. */
  @SSZ private final Hash32 beaconBlockRoot;

  // FFG vote:

  @SSZ private final Checkpoint source;
  @SSZ private final Checkpoint target;

  public AttestationData(
      SlotNumber slot,
      CommitteeIndex index,
      Hash32 beaconBlockRoot,
      Checkpoint source,
      Checkpoint target) {
    this.slot = slot;
    this.index = index;
    this.beaconBlockRoot = beaconBlockRoot;
    this.source = source;
    this.target = target;
  }

  public SlotNumber getSlot() {
    return slot;
  }

  public CommitteeIndex getIndex() {
    return index;
  }

  public Hash32 getBeaconBlockRoot() {
    return beaconBlockRoot;
  }

  public Checkpoint getSource() {
    return source;
  }

  public Checkpoint getTarget() {
    return target;
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
    return Objects.equal(slot, that.slot)
        && Objects.equal(index, that.index)
        && Objects.equal(beaconBlockRoot, that.beaconBlockRoot)
        && Objects.equal(source, that.source)
        && Objects.equal(target, that.target);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(slot, index, beaconBlockRoot, source, target);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("slot", slot)
        .add("index", index)
        .add("beaconBlockRoot", beaconBlockRoot.toStringShort())
        .add("sourceEpoch", source.getEpoch())
        .add("sourceRoot", source.getRoot().toStringShort())
        .add("targetEpoch", target.getEpoch())
        .add("targetRoot", target.getRoot().toStringShort())
        .toString();
  }
}

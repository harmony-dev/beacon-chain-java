package org.ethereum.beacon.core.operations.attestation;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * A Crosslink record.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/0.6.0/specs/core/0_beacon-chain.md#crosslink">Crosslink</a>
 *     in the spec.
 */
@SSZSerializable
public class Crosslink {

  public static final Crosslink EMPTY = new Crosslink(EpochNumber.ZERO, Hash32.ZERO, Hash32.ZERO);

  /** Epoch number. */
  @SSZ private final EpochNumber epoch;
  /** Root of the previous crosslink. */
  @SSZ private final Hash32 previousCrosslinkRoot;
  /** Root of the crosslinked shard data since the previous crosslink. */
  @SSZ private final Hash32 crosslinkDataRoot;

  public Crosslink(EpochNumber epoch, Hash32 previousCrosslinkRoot, Hash32 crosslinkDataRoot) {
    this.epoch = epoch;
    this.previousCrosslinkRoot = previousCrosslinkRoot;
    this.crosslinkDataRoot = crosslinkDataRoot;
  }

  public EpochNumber getEpoch() {
    return epoch;
  }

  public Hash32 getPreviousCrosslinkRoot() {
    return previousCrosslinkRoot;
  }

  public Hash32 getCrosslinkDataRoot() {
    return crosslinkDataRoot;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    Crosslink crosslink = (Crosslink) object;
    return Objects.equal(epoch, crosslink.epoch)
        && Objects.equal(previousCrosslinkRoot, crosslink.previousCrosslinkRoot)
        && Objects.equal(crosslinkDataRoot, crosslink.crosslinkDataRoot);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(epoch, previousCrosslinkRoot, crosslinkDataRoot);
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(SpecConstants spec) {
    return MoreObjects.toStringHelper(this)
        .add("epoch", epoch.toString(spec))
        .add("previousCrosslinkRoot", previousCrosslinkRoot.toStringShort())
        .add("crosslinkDataRoot", crosslinkDataRoot.toStringShort())
        .toString();
  }
}

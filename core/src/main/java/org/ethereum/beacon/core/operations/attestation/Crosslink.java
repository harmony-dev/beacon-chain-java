package org.ethereum.beacon.core.operations.attestation;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * A Crosslink record.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/core/0_beacon-chain.md#crosslink">Crosslink</a>
 *     in the spec.
 */
@SSZSerializable
public class Crosslink {

  public static final Crosslink EMPTY =
      new Crosslink(ShardNumber.ZERO, EpochNumber.ZERO, EpochNumber.ZERO, Hash32.ZERO, Hash32.ZERO);

  /** Shard number. */
  @SSZ private final ShardNumber shard;
  /** Crosslinking data from epochs [start....end-1]. */
@SSZ private final EpochNumber startEpoch;
  @SSZ private final EpochNumber endEpoch;
  /** Root of the previous crosslink. */
  @SSZ private final Hash32 parentRoot;
  /** Root of the crosslinked shard data since the previous crosslink. */
  @SSZ private final Hash32 dataRoot;

  public Crosslink(
      ShardNumber shard,
      EpochNumber startEpoch,
      EpochNumber endEpoch,
      Hash32 parentRoot,
      Hash32 dataRoot) {
    this.shard = shard;
    this.startEpoch = startEpoch;
    this.endEpoch = endEpoch;
    this.parentRoot = parentRoot;
    this.dataRoot = dataRoot;
  }

  public ShardNumber getShard() {
    return shard;
  }

  public EpochNumber getStartEpoch() {
    return startEpoch;
  }

  public EpochNumber getEndEpoch() {
    return endEpoch;
  }

  public Hash32 getParentRoot() {
    return parentRoot;
  }

  public Hash32 getDataRoot() {
    return dataRoot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Crosslink crosslink = (Crosslink) o;
    return Objects.equal(shard, crosslink.shard)
        && Objects.equal(startEpoch, crosslink.startEpoch)
        && Objects.equal(endEpoch, crosslink.endEpoch)
        && Objects.equal(parentRoot, crosslink.parentRoot)
        && Objects.equal(dataRoot, crosslink.dataRoot);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(shard, startEpoch, endEpoch, parentRoot, dataRoot);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("shard", shard)
        .add("startEpoch", startEpoch)
        .add("endEpoch", endEpoch)
        .add("parentRoot", parentRoot.toStringShort())
        .add("dataRoot", dataRoot.toStringShort())
        .toString();
  }
}

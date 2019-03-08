package org.ethereum.beacon.core.operations.slashing;

import com.google.common.base.Objects;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class Proposal {

  @SSZ
  private final SlotNumber slot;
  @SSZ
  private final ShardNumber shard;
  @SSZ
  private final Hash32 blockRoot;
  @SSZ
  private final BLSSignature signature;

  public Proposal(SlotNumber slot, ShardNumber shard, Hash32 blockRoot, BLSSignature blsSignature) {
    this.slot = slot;
    this.shard = shard;
    this.blockRoot = blockRoot;
    this.signature = blsSignature;
  }

  public SlotNumber getSlot() {
    return slot;
  }

  public ShardNumber getShard() {
    return shard;
  }

  public Hash32 getBlockRoot() {
    return blockRoot;
  }

  public BLSSignature getSignature() {
    return signature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Proposal that = (Proposal) o;
    return Objects.equal(slot, that.slot)
        && Objects.equal(shard, that.shard)
        && Objects.equal(blockRoot, that.blockRoot);
  }

  public String toString(@Nullable SpecConstants spec, @Nullable Time beaconStart) {
    return "Proposal["
        + "slot=" + slot.toString(spec, beaconStart)
        + "shard=" + shard.toString(spec)
        + "block=" + blockRoot.toStringShort()
        + "signature=" + signature.toString()
        + "]";
  }
}
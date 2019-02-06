package org.ethereum.beacon.core.operations.slashing;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class ProposalSignedData {
  @SSZ private final SlotNumber slot;
  @SSZ private final ShardNumber shard;
  @SSZ private final Hash32 blockRoot;

  public ProposalSignedData(SlotNumber slot, ShardNumber shard, Hash32 blockRoot) {
    this.slot = slot;
    this.shard = shard;
    this.blockRoot = blockRoot;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProposalSignedData that = (ProposalSignedData) o;
    return Objects.equal(slot, that.slot)
        && Objects.equal(shard, that.shard)
        && Objects.equal(blockRoot, that.blockRoot);
  }
}

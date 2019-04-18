package org.ethereum.beacon.wire.message;

import java.util.List;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class BlockRootsResponseMessage extends MessagePayload {

  @SSZSerializable
  public static class BlockRootSlot {
    @SSZ private final Hash32 blockRoot;
    @SSZ private final SlotNumber slot;

    public BlockRootSlot(Hash32 blockRoot, SlotNumber slot) {
      this.blockRoot = blockRoot;
      this.slot = slot;
    }

    public Hash32 getBlockRoot() {
      return blockRoot;
    }

    public SlotNumber getSlot() {
      return slot;
    }
  }

  @SSZ private final List<BlockRootSlot> roots;

  public BlockRootsResponseMessage(
      List<BlockRootSlot> roots) {
    this.roots = roots;
  }

  public List<BlockRootSlot> getRoots() {
    return roots;
  }
}

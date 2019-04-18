package org.ethereum.beacon.wire.message;

import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BlockRootsRequestMessage extends Message {
  @SSZ private final SlotNumber startSlot;
  @SSZ private final UInt64 count;

  public BlockRootsRequestMessage(SlotNumber startSlot, UInt64 count) {
    this.startSlot = startSlot;
    this.count = count;
  }

  public SlotNumber getStartSlot() {
    return startSlot;
  }

  public UInt64 getCount() {
    return count;
  }
}

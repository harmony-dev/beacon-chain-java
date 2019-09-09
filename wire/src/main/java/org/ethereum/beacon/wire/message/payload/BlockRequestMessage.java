package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.RequestMessagePayload;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BlockRequestMessage extends RequestMessagePayload {
  public static final int METHOD_ID = 0x0D;

  @SSZ private final Hash32 headBlockRoot;
  @SSZ private final SlotNumber startSlot;
  @SSZ private final UInt64 count;
  @SSZ private final UInt64 step;

  public BlockRequestMessage(Hash32 headBlockRoot,
      SlotNumber startSlot, UInt64 count, UInt64 step) {
    this.headBlockRoot = headBlockRoot;
    this.startSlot = startSlot;
    this.count = count;
    this.step = step;
  }

  @Override
  public int getMethodId() {
    return METHOD_ID;
  }

  public Hash32 getHeadBlockRoot() {
    return headBlockRoot;
  }

  public SlotNumber getStartSlot() {
    return startSlot;
  }

  public UInt64 getCount() {
    return count;
  }

  public UInt64 getStep() {
    return step;
  }

  @Override
  public String toString() {
    return "BlockRequestMessage{" +
        "headBlockRoot=" + headBlockRoot +
        ", startSlot=" + startSlot +
        ", count=" + count +
        ", step=" + step +
        '}';
  }
}

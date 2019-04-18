package org.ethereum.beacon.wire.message;

import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BlockHeadersRequestMessage extends RequestMessagePayload {
  public static final UInt64 METHOD_ID = UInt64.valueOf(0x0D);


  @SSZ private final Hash32 startRoot;
  @SSZ private final SlotNumber startSlot;
  @SSZ private final UInt64 maxHeaders;
  @SSZ private final UInt64 skipSlots;

  public BlockHeadersRequestMessage(Hash32 startRoot,
      SlotNumber startSlot, UInt64 maxHeaders, UInt64 skipSlots) {
    this.startRoot = startRoot;
    this.startSlot = startSlot;
    this.maxHeaders = maxHeaders;
    this.skipSlots = skipSlots;
  }

  @Override
  public UInt64 getMethodId() {
    return METHOD_ID;
  }

  public Hash32 getStartRoot() {
    return startRoot;
  }

  public SlotNumber getStartSlot() {
    return startSlot;
  }

  public UInt64 getMaxHeaders() {
    return maxHeaders;
  }

  public UInt64 getSkipSlots() {
    return skipSlots;
  }
}

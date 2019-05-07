package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.MessagePayload;
import org.ethereum.beacon.wire.message.RequestMessagePayload;

@SSZSerializable
public class NotifyNewBlockMessage extends RequestMessagePayload {
  public static final int METHOD_ID = 0xF01;

  @SSZ private final BeaconBlock block;

  public NotifyNewBlockMessage(BeaconBlock block) {
    this.block = block;
  }

  public BeaconBlock getBlock() {
    return block;
  }

  @Override
  public int getMethodId() {
    return METHOD_ID;
  }
}

package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.MessagePayload;
import org.ethereum.beacon.wire.message.RequestMessagePayload;

@SSZSerializable
public class NotifyNewBlockMessage extends RequestMessagePayload {
  public static final int METHOD_ID = 0xF01;

  @SSZ private final SignedBeaconBlock block;

  public NotifyNewBlockMessage(SignedBeaconBlock block) {
    this.block = block;
  }

  public SignedBeaconBlock getBlock() {
    return block;
  }

  @Override
  public int getMethodId() {
    return METHOD_ID;
  }

  @Override
  public String toString() {
    return "NotifyNewBlockMessage{" +
        "block=" + block +
        '}';
  }
}

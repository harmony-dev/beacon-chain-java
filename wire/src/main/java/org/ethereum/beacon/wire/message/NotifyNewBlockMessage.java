package org.ethereum.beacon.wire.message;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class NotifyNewBlockMessage extends MessagePayload {
  @SSZ private final BeaconBlock block;

  public NotifyNewBlockMessage(BeaconBlock block) {
    this.block = block;
  }

  public BeaconBlock getBlock() {
    return block;
  }
}

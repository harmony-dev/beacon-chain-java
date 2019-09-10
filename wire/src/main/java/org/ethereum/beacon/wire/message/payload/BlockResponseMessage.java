package org.ethereum.beacon.wire.message.payload;

import java.util.List;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.ResponseMessagePayload;

@SSZSerializable(skipContainer = true)
public class BlockResponseMessage extends ResponseMessagePayload {

  @SSZ private final List<BeaconBlock> blocks;

  public BlockResponseMessage(List<BeaconBlock> blocks) {
    this.blocks = blocks;
  }

  public List<BeaconBlock> getBlocks() {
    return blocks;
  }

  @Override
  public String toString() {
    return "BlockResponseMessage{" +
        "blocks=" + blocks +
        '}';
  }
}

package org.ethereum.beacon.wire.message;

import java.util.List;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class BlockBodiesResponseMessage extends MessagePayload {
  @SSZ private final List<BeaconBlockBody> blockBodies;

  public BlockBodiesResponseMessage(List<BeaconBlockBody> blockBodies) {
    this.blockBodies = blockBodies;
  }

  public List<BeaconBlockBody> getBlockBodies() {
    return blockBodies;
  }
}

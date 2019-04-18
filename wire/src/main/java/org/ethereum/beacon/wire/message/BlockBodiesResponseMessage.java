package org.ethereum.beacon.wire.message;

import java.util.List;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class BlockBodiesResponseMessage extends ResponseMessagePayload<BlockBodiesRequestMessage> {

  @SSZ private final List<BeaconBlockBody> blockBodies;

  public BlockBodiesResponseMessage(
      BlockBodiesRequestMessage request,
      List<BeaconBlockBody> blockBodies) {
    super(request);
    this.blockBodies = blockBodies;
  }

  public List<BeaconBlockBody> getBlockBodies() {
    return blockBodies;
  }
}

package org.ethereum.beacon.wire.message.payload;

import java.util.List;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.RequestMessagePayload;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BlockBodiesRequestMessage extends RequestMessagePayload {
  public static final int METHOD_ID = 0x0E;

  @SSZ private final List<Hash32> blockTreeRoots;

  public BlockBodiesRequestMessage(
      List<Hash32> blockTreeRoots) {
    this.blockTreeRoots = blockTreeRoots;
  }

  @Override
  public int getMethodId() {
    return METHOD_ID;
  }

  public List<Hash32> getBlockTreeRoots() {
    return blockTreeRoots;
  }

  @Override
  public String toString() {
    return "BlockBodiesRequestMessage{" +
        "blockTreeRoots=" + blockTreeRoots +
        '}';
  }
}

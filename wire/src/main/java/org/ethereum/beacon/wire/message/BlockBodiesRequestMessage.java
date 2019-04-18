package org.ethereum.beacon.wire.message;

import java.util.List;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BlockBodiesRequestMessage extends Message {
  @SSZ private final List<Hash32> blockTreeRoots;

  public BlockBodiesRequestMessage(
      List<Hash32> blockTreeRoots) {
    this.blockTreeRoots = blockTreeRoots;
  }

  public List<Hash32> getBlockTreeRoots() {
    return blockTreeRoots;
  }
}

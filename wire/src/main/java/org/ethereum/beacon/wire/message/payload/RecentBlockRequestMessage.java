package org.ethereum.beacon.wire.message.payload;

import java.util.List;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.RequestMessagePayload;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class RecentBlockRequestMessage extends RequestMessagePayload {
  public static final int METHOD_ID = 0x0E;

  @SSZ private final List<Hash32> blockRoots;

  public RecentBlockRequestMessage(
      List<Hash32> blockRoots) {
    this.blockRoots = blockRoots;
  }

  @Override
  public int getMethodId() {
    return METHOD_ID;
  }

  public List<Hash32> getBlockRoots() {
    return blockRoots;
  }

  @Override
  public String toString() {
    return "RecentBlockRequestMessage{" +
        "blockRoots=" + blockRoots +
        '}';
  }
}

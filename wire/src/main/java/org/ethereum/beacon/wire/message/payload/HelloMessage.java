package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.RequestMessagePayload;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes4;

@SSZSerializable
public class HelloMessage extends RequestMessagePayload {
  public static final int METHOD_ID = 0x0;

  @SSZ private final Bytes4 fork;
  @SSZ private final Hash32 finalizedRoot;
  @SSZ private final EpochNumber finalizedEpoch;
  @SSZ private final Hash32 headRoot;
  @SSZ private final SlotNumber headSlot;

  public HelloMessage(Bytes4 fork, Hash32 finalizedRoot,
      EpochNumber finalizedEpoch, Hash32 headRoot, SlotNumber headSlot) {
    this.fork = fork;
    this.finalizedRoot = finalizedRoot;
    this.finalizedEpoch = finalizedEpoch;
    this.headRoot = headRoot;
    this.headSlot = headSlot;
  }

  @Override
  public int getMethodId() {
    return METHOD_ID;
  }

  public Bytes4 getFork() {
    return fork;
  }

  public Hash32 getFinalizedRoot() {
    return finalizedRoot;
  }

  public EpochNumber getFinalizedEpoch() {
    return finalizedEpoch;
  }

  public Hash32 getHeadRoot() {
    return headRoot;
  }

  public SlotNumber getHeadSlot() {
    return headSlot;
  }

  @Override
  public String toString() {
    return "HelloMessage{" +
        "fork=" + fork +
        ", finalizedRoot=" + finalizedRoot +
        ", finalizedEpoch=" + finalizedEpoch +
        ", headRoot=" + headRoot +
        ", headSlot=" + headSlot +
        '}';
  }
}

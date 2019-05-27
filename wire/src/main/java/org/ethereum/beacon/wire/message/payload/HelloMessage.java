package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.RequestMessagePayload;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class HelloMessage extends RequestMessagePayload {
  public static final int METHOD_ID = 0x0;

  @SSZ(type = "uint8")
  private final int networkId;
  @SSZ private final UInt64 chainId;
  @SSZ private final Hash32 latestFinalizedRoot;
  @SSZ private final EpochNumber latestFinalizedEpoch;
  @SSZ private final Hash32 bestRoot;
  @SSZ private final SlotNumber bestSlot;

  public HelloMessage(int networkId, UInt64 chainId,
      Hash32 latestFinalizedRoot, EpochNumber latestFinalizedEpoch,
      Hash32 bestRoot, SlotNumber bestSlot) {
    this.networkId = networkId;
    this.chainId = chainId;
    this.latestFinalizedRoot = latestFinalizedRoot;
    this.latestFinalizedEpoch = latestFinalizedEpoch;
    this.bestRoot = bestRoot;
    this.bestSlot = bestSlot;
  }

  @Override
  public int getMethodId() {
    return METHOD_ID;
  }

  public int getNetworkId() {
    return (byte) networkId;
  }

  public UInt64 getChainId() {
    return chainId;
  }

  public Hash32 getLatestFinalizedRoot() {
    return latestFinalizedRoot;
  }

  public EpochNumber getLatestFinalizedEpoch() {
    return latestFinalizedEpoch;
  }

  public Hash32 getBestRoot() {
    return bestRoot;
  }

  public SlotNumber getBestSlot() {
    return bestSlot;
  }

  @Override
  public String toString() {
    return "HelloMessage{" +
        "networkId=" + networkId +
        ", chainId=" + chainId +
        ", latestFinalizedRoot=" + latestFinalizedRoot +
        ", latestFinalizedEpoch=" + latestFinalizedEpoch +
        ", bestRoot=" + bestRoot +
        ", bestSlot=" + bestSlot +
        '}';
  }
}

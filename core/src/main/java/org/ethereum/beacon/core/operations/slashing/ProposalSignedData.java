package org.ethereum.beacon.core.operations.slashing;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class ProposalSignedData {
  private final UInt64 slot;
  private final UInt64 shard;
  private final Hash32 blockRoot;

  public ProposalSignedData(UInt64 slot, UInt64 shard, Hash32 blockRoot) {
    this.slot = slot;
    this.shard = shard;
    this.blockRoot = blockRoot;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public UInt64 getShard() {
    return shard;
  }

  public Hash32 getBlockRoot() {
    return blockRoot;
  }
}

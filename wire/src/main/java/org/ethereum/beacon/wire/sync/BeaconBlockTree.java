package org.ethereum.beacon.wire.sync;

import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.sync.BeaconBlockTree.BlockWrapper;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconBlockTree extends AbstractBlockTree<Hash32, BlockWrapper, Feedback<SignedBeaconBlock>> {

  private final ObjectHasher<Hash32> hasher;

  protected class BlockWrapper implements AbstractBlockTree.BlockWrap<Hash32, Feedback<SignedBeaconBlock>> {
    private final Feedback<SignedBeaconBlock> block;

    public BlockWrapper(Feedback<SignedBeaconBlock> block) {
      this.block = block;
    }

    @Override
    public Hash32 getHash() {
      return hasher.getHash(block.get().getMessage());
    }

    @Override
    public Hash32 getParentHash() {
      return block.get().getMessage().getParentRoot();
    }

    @Override
    public long getHeight() {
      return block.get().getMessage().getSlot().longValue();
    }

    @Override
    public Feedback<SignedBeaconBlock> get() {
      return block;
    }
  }

  public BeaconBlockTree(ObjectHasher<Hash32> hasher) {
    this.hasher = hasher;
  }

  @Override
  protected BlockWrapper wrap(Feedback<SignedBeaconBlock> origBlock) {
    return new BlockWrapper(origBlock);
  }
}

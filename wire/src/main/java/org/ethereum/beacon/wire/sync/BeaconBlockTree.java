package org.ethereum.beacon.wire.sync;

import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.sync.BeaconBlockTree.BlockWrapper;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconBlockTree extends AbstractBlockTree<Hash32, BlockWrapper, Feedback<BeaconBlock>> {

  private final ObjectHasher<Hash32> hasher;

  protected class BlockWrapper implements AbstractBlockTree.BlockWrap<Hash32, Feedback<BeaconBlock>> {
    private final Feedback<BeaconBlock> block;

    public BlockWrapper(Feedback<BeaconBlock> block) {
      this.block = block;
    }

    @Override
    public Hash32 getHash() {
      return hasher.getHash(block.get());
    }

    @Override
    public Hash32 getParentHash() {
      return block.get().getPreviousBlockRoot();
    }

    @Override
    public long getHeight() {
      return block.get().getSlot().longValue();
    }

    @Override
    public Feedback<BeaconBlock> get() {
      return block;
    }
  }

  public BeaconBlockTree(ObjectHasher<Hash32> hasher) {
    this.hasher = hasher;
  }

  @Override
  protected BlockWrapper wrap(Feedback<BeaconBlock> origBlock) {
    return new BlockWrapper(origBlock);
  }
}

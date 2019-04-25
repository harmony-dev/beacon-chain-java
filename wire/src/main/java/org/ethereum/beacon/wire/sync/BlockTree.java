package org.ethereum.beacon.wire.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.ethereum.beacon.wire.sync.BlockTree.Block;

public interface BlockTree<THash, TBlock extends Block<THash>> {

  interface Block<THash> {

    THash getHash();

    THash getParentHash();

    long getHeight();
  }

  @Nonnull List<TBlock> addBlock(@Nonnull TBlock block);

  default List<TBlock> addChainedBlocks(Collection<TBlock> blocks) {
    List<TBlock> ret = new ArrayList<>();
    for (TBlock block : blocks) {
      ret.addAll(addBlock(block));
    }
    return ret;
  }

  void setTopBlock(@Nonnull TBlock block);

  @Nonnull TBlock getTopBlock();
}

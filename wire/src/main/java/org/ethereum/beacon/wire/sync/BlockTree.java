package org.ethereum.beacon.wire.sync;

import java.util.List;
import javax.annotation.Nonnull;
import org.ethereum.beacon.wire.sync.BlockTree.Block;

/**
 * Builds a tree of added blocks returning block chains linked to the top block
 */
public interface BlockTree<THash, TBlock extends Block<THash>> {

  /**
   * Abstract blockchain Block which has a Hash, a Parent and a Height
   */
  interface Block<THash> {

    THash getHash();

    THash getParentHash();

    long getHeight();
  }

  /**
   * Adds a new block to the block tree and returns a list of blocks
   * (in order of their inheritance - first parents then children)
   * which became linked to the {@link #getTopBlock()} due to adding this
   * block (including the block itself).
   * All blocks returned across all calls to this method ar unique, i.e. no
   * block returned twice.
   * Any block returned from this method is connected to <b>initial TopBlock</b>
   * with blocks already returned from this method before.
   * E.g.
   * - if the supplied block has no parents in the current tree, then block is stored
   *   but empty list is returned
   * - if the supplied block has a parent but no existing children then only this block is
   *    returned
   * - if the supplied block has a parent and a number of descendants then
   *   this block + all its descendants returned
   *
   * Blocks with height less than {@link #getTopBlock()} are dropped
   * Blocks with height bigger than {@link #getTopBlock()} + threshold are dropped
   * Duplicate blocks are ignored
   */
  @Nonnull List<TBlock> addBlock(@Nonnull TBlock block);

  /**
   * Sets the top final root block
   * All blocks with height less than top block height are removed from the tree
   */
  void setTopBlock(@Nonnull TBlock block);

  /**
   * Returns current Top Block
   */
  @Nonnull TBlock getTopBlock();
}

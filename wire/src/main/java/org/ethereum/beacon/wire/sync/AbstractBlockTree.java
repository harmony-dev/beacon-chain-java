package org.ethereum.beacon.wire.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.wire.sync.AbstractBlockTree.BlockWrap;

public abstract class AbstractBlockTree<THash, TBlock extends BlockWrap<THash, TRawBlock>, TRawBlock>
    implements BlockTree<THash, TBlock> {

  interface BlockWrap<THash, TRawBlock> extends Block<THash> {
    TRawBlock get();
  }

  private static final Logger logger = LogManager.getLogger(AbstractBlockTree.class);

  private TBlock topBlock;
  private final Map<THash, TBlock> hashMap = new HashMap<>();
  private final Map<THash, List<THash>> childrenMap = new HashMap<>();

  protected abstract TBlock wrap(TRawBlock origBlock);

  public List<TRawBlock> addBlock(TRawBlock block) {
    return addBlock(wrap(block)).stream().map(BlockWrap::get).collect(Collectors.toList());
  }

  public void setTopBlock(TRawBlock block) {
    setTopBlock(wrap(block));
  }

  @Nonnull
  @Override
  public synchronized List<TBlock> addBlock(@Nonnull TBlock block) {
    if (topBlock == null) {
      throw new IllegalStateException("Top block should be set first");
    }
    try {
      if (hashMap.containsKey(block.getHash())) return Collections.emptyList();
      if (topBlock.getHeight() >= block.getHeight()) return Collections.emptyList();
      hashMap.put(block.getHash(), block);
      childrenMap.computeIfAbsent(block.getParentHash(), r -> new ArrayList<>()).add(block.getHash());

      List<TBlock> ret = new ArrayList<>();
      if (isRootSuccessor(block)) {
        ret.add(block);
        addChildrenRecursively(block.getHash(), ret);
      }
      logger.debug("Returning " + ret.size() + " ready blocks on added block " + block + " ~~> " + ret);
      return ret;
    } catch (Exception e) {
      logger.error("Exception adding block: " + block, e);
      throw new RuntimeException(e);
    }
  }

  private boolean isRootSuccessor(TBlock block) {
    while (block != null) {
      if (block.getParentHash().equals(topBlock.getHash())) {
        return true;
      }
      block = hashMap.get(block.getParentHash());
    }
    return false;
  }

  private void addChildrenRecursively(THash blockHash, List<TBlock> successors) {
    List<THash> blockChildren = childrenMap.getOrDefault(blockHash, Collections.emptyList());
    for (THash childHash : blockChildren) {
      successors.add(hashMap.get(childHash));
      addChildrenRecursively(childHash, successors);
    }
  }

  @Override
  public synchronized void setTopBlock(@Nonnull TBlock block) {
    if (topBlock != null) {
      if (!hashMap.containsKey(block.getHash())) {
        throw new IllegalArgumentException(
            "setTopBlock() should be called with existing block or to initialize");
      }
      Iterator<Entry<THash, TBlock>> iterator = hashMap.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<THash, TBlock> entry = iterator.next();
        if (entry.getValue().getHeight() <= block.getHeight()
            && !entry.getKey().equals(block.getHash())) {
          iterator.remove();
          childrenMap.remove(entry.getKey());
        }
      }
    }
    topBlock = block;
  }

  @Nonnull
  @Override
  public synchronized TBlock getTopBlock() {
    return topBlock;
  }
}

package org.ethereum.beacon.chain.storage.impl;

import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.HoleyList;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class BeaconBlockStorageImpl implements BeaconBlockStorage {

  private static class SlotBlocks {

    private final List<Hash32> blockHashes;
    private final Hash32 justifiedHash;
    private final Hash32 finalizedHash;

    SlotBlocks(Hash32 blockHash) {
      this(singletonList(blockHash), null, null);
    }

    SlotBlocks(Hash32 blockHash, Hash32 justifiedHash, Hash32 finalizedHash) {
      this(singletonList(blockHash), justifiedHash, finalizedHash);
    }

    public SlotBlocks(List<Hash32> blockHashes, Hash32 justifiedHash, Hash32 finalizedHash) {
      this.blockHashes = blockHashes;
      this.justifiedHash = justifiedHash;
      this.finalizedHash = finalizedHash;
    }

    public List<Hash32> getBlockHashes() {
      return blockHashes;
    }

    public Hash32 getJustifiedHash() {
      return justifiedHash;
    }

    public Hash32 getFinalizedHash() {
      return finalizedHash;
    }

    SlotBlocks addBlock(Hash32 newBlock) {
      ArrayList<Hash32> blocks = new ArrayList<>(getBlockHashes());
      blocks.add(newBlock);
      return new SlotBlocks(blocks, justifiedHash, finalizedHash);
    }

    SlotBlocks setJustifiedHash(Hash32 newJustifiedHash) {
      return new SlotBlocks(blockHashes, newJustifiedHash, finalizedHash);
    }

    SlotBlocks setFinalizedHash(Hash32 newFinalizedHash) {
      return new SlotBlocks(blockHashes, justifiedHash, newFinalizedHash);
    }

    @Override
    public String toString() {
      return "SlotBlocks{"
          + "blockHashes="
          + blockHashes
          + ", justifiedHash="
          + justifiedHash
          + ", finalizedHash="
          + finalizedHash
          + '}';
    }
  }

  private final DataSource<Hash32, BeaconBlock> rawBlocks;
  private final HoleyList<SlotBlocks> blockIndex;
  private final ChainSpec chainSpec;
  private final boolean checkBlockExistOnAdd;
  private final boolean checkParentExistOnAdd;

  public BeaconBlockStorageImpl(DataSource<Hash32, BeaconBlock> rawBlocks,
                                HoleyList<SlotBlocks> blockIndex,
                                ChainSpec chainSpec) {
    this(rawBlocks, blockIndex, chainSpec, true, true);
  }

  /**
   * @param rawBlocks hash -> block datasource
   * @param blockIndex slot -> blocks datasource
   * @param chainSpec Chain specification
   * @param checkBlockExistOnAdd asserts that no duplicate blocks added (adds some overhead)
   * @param checkParentExistOnAdd asserts that added block parent is already here (adds some overhead)
   */
  public BeaconBlockStorageImpl(DataSource<Hash32, BeaconBlock> rawBlocks,
                                HoleyList<SlotBlocks> blockIndex,
                                ChainSpec chainSpec,
                                boolean checkBlockExistOnAdd,
                                boolean checkParentExistOnAdd) {
    this.rawBlocks = rawBlocks;
    this.blockIndex = blockIndex;
    this.chainSpec = chainSpec;
    this.checkBlockExistOnAdd = checkBlockExistOnAdd;
    this.checkParentExistOnAdd = checkParentExistOnAdd;
  }

  @Override
  public long getMaxSlot() {
    return blockIndex.size() - 1;
  }

  @Override
  public List<Hash32> getSlotBlocks(long slot) {
    return blockIndex.get(slot)
        .map(SlotBlocks::getBlockHashes)
        .orElse(Collections.emptyList());
  }

  @Override
  public Optional<BeaconBlock> get(@Nonnull Hash32 key) {
    return rawBlocks.get(key);
  }

  @Override
  public void put(@Nonnull Hash32 key, @Nonnull BeaconBlock newBlock) {
    if (checkBlockExistOnAdd) {
      if (get(key).isPresent()) {
        throw new IllegalArgumentException("Block with hash already exists in storage: " + newBlock);
      }
    }

    if (!isEmpty() && checkParentExistOnAdd) {
      if (!get(newBlock.getParentRoot()).isPresent()) {
        throw new IllegalArgumentException("No parent found for added block: " + newBlock);
      }
    }

    rawBlocks.put(key, newBlock);
    SlotBlocks slotBlocks =
        newBlock.getSlot() == chainSpec.getGenesisSlot()
            ? new SlotBlocks(newBlock.getHash(), newBlock.getHash(), newBlock.getHash())
            : new SlotBlocks(newBlock.getHash());
    blockIndex.update(
        newBlock.getSlot().getValue(),
        blocks -> blocks.addBlock(newBlock.getHash()),
        () -> slotBlocks);
  }

  @Override
  public void remove(@Nonnull Hash32 key) {
    Optional<BeaconBlock> block = rawBlocks.get(key);
    if (block.isPresent()) {
      rawBlocks.remove(key);
      SlotBlocks slotBlocks = blockIndex.get(block.get().getSlot().getValue()).orElseThrow(
          () -> new IllegalStateException("Internal error: rawBlocks contains block, but blockIndex misses: " + key));
      int idx = 0;
      for (; idx < slotBlocks.getBlockHashes().size(); idx++) {
        if (slotBlocks.getBlockHashes().get(idx).equals(key)) {
          break;
        }
      }
      ArrayList<Hash32> newBlocks = new ArrayList<>(slotBlocks.getBlockHashes());
      newBlocks.remove(idx);
      Hash32 justifiedHash =
          slotBlocks.justifiedHash != null && slotBlocks.justifiedHash != key
              ? slotBlocks.justifiedHash
              : null;
      Hash32 finalizedHash =
          slotBlocks.finalizedHash != null && slotBlocks.finalizedHash != key
              ? slotBlocks.finalizedHash
              : null;
      blockIndex.put(block.get().getSlot().getValue(), new SlotBlocks(newBlocks, justifiedHash, finalizedHash));
    }
  }

  @Override
  public List<BeaconBlock> getChildren(@Nonnull Hash32 parent, int limit) {
    Optional<BeaconBlock> block = get(parent);
    if (!block.isPresent()) {
      return Collections.emptyList();
    }
    BeaconBlock start = block.get();
    final List<BeaconBlock> children = new ArrayList<>();

    for (long curSlot = start.getSlot().getValue() + 1;
        curSlot <= Math.min(start.getSlot().getValue() + limit, getMaxSlot());
        ++curSlot) {
      getSlotBlocks(curSlot).stream()
          .map(this::get)
          .filter(Optional::isPresent)
          .filter(b -> start.isParentOf(b.get()))
          .forEach(b -> children.add(b.get()));
    }

    return children;
  }

  public boolean justify(Hash32 blockHash) {
    Optional<UInt64> slot = get(blockHash).map(BeaconBlock::getSlot);
    if (!slot.isPresent()) {
      return false;
    }

    return blockIndex
        .get(slot.get().getValue())
        .flatMap(slotBlocks -> Optional.of(slotBlocks.setJustifiedHash(blockHash)))
        .map(
            slotBlocks -> {
              blockIndex.put(slot.get().getValue(), slotBlocks);
              return true;
            })
        .orElse(false);
  }

  public boolean finalize(Hash32 blockHash) {
    Optional<UInt64> slot = get(blockHash).map(BeaconBlock::getSlot);
    if (!slot.isPresent()) {
      return false;
    }

    return blockIndex
        .get(slot.get().getValue())
        .flatMap(slotBlocks -> Optional.of(slotBlocks.setFinalizedHash(blockHash)))
        .map(
            slotBlocks -> {
              blockIndex.put(slot.get().getValue(), slotBlocks);
              return true;
            })
        .orElse(false);
  }

  @Override
  public Optional<BeaconBlock> getJustifiedBlock(long slot, int limit) {
    for (long i = slot; i >= Math.max(0, slot - limit); --i) {
      Optional<SlotBlocks> slotBlocksOptional = blockIndex.get(i);
      if (slotBlocksOptional.isPresent()) {
        SlotBlocks slotBlocks = slotBlocksOptional.get();
        if (slotBlocks.justifiedHash != null) {
          return get(slotBlocks.justifiedHash);
        }
      }
    }

    return Optional.empty();
  }

  @Override
  public Optional<BeaconBlock> getFinalizedBlock(long slot, int limit) {
    for (long i = slot; i >= Math.max(0, slot - limit); --i) {
      Optional<SlotBlocks> slotBlocksOptional = blockIndex.get(i);
      if (slotBlocksOptional.isPresent()) {
        SlotBlocks slotBlocks = slotBlocksOptional.get();
        if (slotBlocks.finalizedHash != null) {
          return get(slotBlocks.finalizedHash);
        }
      }
    }

    return Optional.empty();
  }

  @Override
  public void flush() {
    // nothing to be done here. No cached data in this implementation
  }
}

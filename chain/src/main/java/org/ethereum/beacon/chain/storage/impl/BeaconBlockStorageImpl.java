package org.ethereum.beacon.chain.storage.impl;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.ethereum.beacon.chain.storage.AbstractHashKeyStorage;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.source.CodecSource;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.HoleyList;
import org.ethereum.beacon.db.source.impl.DataSourceList;
import org.ethereum.beacon.ssz.annotation.SSZ;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconBlockStorageImpl extends AbstractHashKeyStorage<Hash32, BeaconBlock>
    implements BeaconBlockStorage {

  private static class SlotBlocks {

    @SSZ private final List<Hash32> blockHashes;
    @SSZ private final List<Hash32> justifiedHashes;
    @SSZ private final Hash32 finalizedHash;

    SlotBlocks(Hash32 blockHash) {
      this(singletonList(blockHash), Collections.emptyList(), null);
    }

    SlotBlocks(Hash32 blockHash, Hash32 justifiedHash, Hash32 finalizedHash) {
      this(singletonList(blockHash), singletonList(justifiedHash), finalizedHash);
    }

    SlotBlocks(Hash32 blockHash, List<Hash32> justifiedHashes, Hash32 finalizedHash) {
      this(singletonList(blockHash), justifiedHashes, finalizedHash);
    }

    public SlotBlocks(List<Hash32> blockHashes, List<Hash32> justifiedHashes, Hash32 finalizedHash) {
      this.blockHashes = blockHashes;
      this.justifiedHashes = justifiedHashes;
      this.finalizedHash = finalizedHash;
    }

    public SlotBlocks(List<Hash32> blockHashes, Hash32 justifiedHash, Hash32 finalizedHash) {
      this.blockHashes = blockHashes;
      this.justifiedHashes = singletonList(justifiedHash);
      this.finalizedHash = finalizedHash;
    }

    public List<Hash32> getBlockHashes() {
      return blockHashes;
    }

    public List<Hash32> getJustifiedHashes() {
      return justifiedHashes;
    }

    public Hash32 getFinalizedHash() {
      return finalizedHash;
    }

    SlotBlocks addBlock(Hash32 newBlock) {
      List<Hash32> blocks = new ArrayList<>(getBlockHashes());
      blocks.add(newBlock);
      return new SlotBlocks(blocks, justifiedHashes, finalizedHash);
    }

    SlotBlocks addJustifiedHash(Hash32 newJustifiedHash) {
      List<Hash32> justifiedHash = new ArrayList<>(getJustifiedHashes());
      justifiedHash.add(newJustifiedHash);
      return new SlotBlocks(blockHashes, justifiedHash, finalizedHash);
    }

    SlotBlocks removeJustifiedHash(Hash32 badJustifiedHash) {
      List<Hash32> justifiedHash =
          getJustifiedHashes().stream()
              .filter(hash -> !badJustifiedHash.equals(hash))
              .collect(Collectors.toList());
      return new SlotBlocks(blockHashes, justifiedHash, finalizedHash);
    }

    SlotBlocks setFinalizedHash(Hash32 newFinalizedHash) {
      return new SlotBlocks(blockHashes, justifiedHashes, newFinalizedHash);
    }

    @Override
    public String toString() {
      return "SlotBlocks{"
          + "blockHashes="
          + blockHashes
          + ", justifiedHashes="
          + justifiedHashes
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

  public BeaconBlockStorageImpl(
      ObjectHasher<Hash32> objectHasher,
      DataSource<Hash32, BeaconBlock> rawBlocks,
      HoleyList<SlotBlocks> blockIndex,
      ChainSpec chainSpec) {
    this(objectHasher, rawBlocks, blockIndex, chainSpec, true, true);
  }

  /**
   * @param rawBlocks hash -> block datasource
   * @param blockIndex slot -> blocks datasource
   * @param chainSpec Chain specification
   * @param checkBlockExistOnAdd asserts that no duplicate blocks added (adds some overhead)
   * @param checkParentExistOnAdd asserts that added block parent is already here (adds some
   *     overhead)
   */
  public BeaconBlockStorageImpl(
      ObjectHasher<Hash32> objectHasher,
      DataSource<Hash32, BeaconBlock> rawBlocks,
      HoleyList<SlotBlocks> blockIndex,
      ChainSpec chainSpec,
      boolean checkBlockExistOnAdd,
      boolean checkParentExistOnAdd) {
    super(objectHasher);
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
    return blockIndex
        .get(slot)
        .map(slotBlocks -> (List<Hash32>) new ArrayList<>(slotBlocks.getBlockHashes()))
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
        throw new IllegalArgumentException(
            "Block with hash already exists in storage: " + newBlock);
      }
    }

    if (!isEmpty() && checkParentExistOnAdd) {
      if (!get(newBlock.getParentRoot()).isPresent()) {
        throw new IllegalArgumentException("No parent found for added block: " + newBlock);
      }
    }

    rawBlocks.put(key, newBlock);
    Hash32 newBlockHash = getObjectHasher().getHash(newBlock);
    SlotBlocks slotBlocks =
        newBlock.getSlot().equals(chainSpec.getGenesisSlot())
            ? new SlotBlocks(newBlockHash, newBlockHash, newBlockHash)
            : new SlotBlocks(newBlockHash);
    blockIndex.update(
        newBlock.getSlot().getValue(),
        blocks -> blocks.addBlock(newBlockHash),
        () -> slotBlocks);
  }

  @Override
  public void remove(@Nonnull Hash32 key) {
    Optional<BeaconBlock> block = rawBlocks.get(key);
    if (block.isPresent()) {
      rawBlocks.remove(key);
      SlotBlocks slotBlocks =
          blockIndex
              .get(block.get().getSlot().getValue())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Internal error: rawBlocks contains block, but blockIndex misses: "
                              + key));
      List<Hash32> newBlocks = new ArrayList<>(slotBlocks.getBlockHashes());
      newBlocks.remove(key);
      List<Hash32> justifiedHashes =
          slotBlocks.getJustifiedHashes().stream()
              .filter(hash -> !key.equals(hash))
              .collect(Collectors.toList());
      Hash32 finalizedHash =
          slotBlocks.finalizedHash != null && slotBlocks.finalizedHash != key
              ? slotBlocks.finalizedHash
              : null;
      blockIndex.put(
          block.get().getSlot().getValue(),
          new SlotBlocks(newBlocks, justifiedHashes, finalizedHash));
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
          .filter(b -> isChild(start, b.get()))
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
        .flatMap(slotBlocks -> Optional.of(slotBlocks.addJustifiedHash(blockHash)))
        .map(
            slotBlocks -> {
              blockIndex.put(slot.get().getValue(), slotBlocks);
              return true;
            })
        .orElse(false);
  }

  @Override
  public boolean deJustify(Hash32 blockHash) {
    Optional<UInt64> slot = get(blockHash).map(BeaconBlock::getSlot);
    if (!slot.isPresent()) {
      return false;
    }

    return blockIndex
        .get(slot.get().getValue())
        .flatMap(slotBlocks -> Optional.of(slotBlocks.removeJustifiedHash(blockHash)))
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
        // FIXME: stub. There could be a case when we have 2 justified blocks (one should be
        // slashed) and need to deal with it
        return slotBlocks.justifiedHashes.stream().findFirst().flatMap(this::get);
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

  private boolean isChild(BeaconBlock parent, BeaconBlock child) {
    return getObjectHasher().getHash(parent).equals(child.getParentRoot());
  }

  public static BeaconBlockStorageImpl create(
      Database database,
      ObjectHasher<Hash32> objectHasher,
      SerializerFactory serializerFactory,
      ChainSpec chainSpec) {
    DataSource<BytesValue, BytesValue> backingBlockSource = database.createStorage("beacon-block");
    DataSource<BytesValue, BytesValue> backingIndexSource =
        database.createStorage("beacon-block-index");

    DataSource<Hash32, BeaconBlock> blockSource =
        new CodecSource<>(
            backingBlockSource,
            key -> key,
            serializerFactory.getSerializer(BeaconBlock.class),
            serializerFactory.getDeserializer(BeaconBlock.class));
    HoleyList<SlotBlocks> indexSource =
        new DataSourceList<>(
            backingIndexSource,
            serializerFactory.getSerializer(SlotBlocks.class),
            serializerFactory.getDeserializer(SlotBlocks.class));

    return new BeaconBlockStorageImpl(objectHasher, blockSource, indexSource, chainSpec);
  }
}

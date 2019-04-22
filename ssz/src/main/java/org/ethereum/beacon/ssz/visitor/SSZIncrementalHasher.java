package org.ethereum.beacon.ssz.visitor;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ethereum.beacon.ssz.incremental.ObservableComposite;
import org.ethereum.beacon.ssz.incremental.UpdateListener;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer.SSZSerializerResult;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SSZIncrementalHasher extends SSZSimpleHasher {
  private static final String INCREMENTAL_HASHER_OBSERVER_ID = "Hasher";

  static class SSZIncrementalTracker implements UpdateListener {
    TreeSet<Integer> elementsUpdated = new TreeSet<>();
    MerkleTrie merkleTree;

    public SSZIncrementalTracker(TreeSet<Integer> elementsUpdated,
        MerkleTrie merkleTree) {
      this.elementsUpdated = elementsUpdated;
      this.merkleTree = merkleTree;
    }

    public SSZIncrementalTracker() {
    }

    @Override
    public void childUpdated(int childIndex) {
      elementsUpdated.add(childIndex);
    }

    @Override
    public UpdateListener fork() {
      return new SSZIncrementalTracker(
          (TreeSet<Integer>) elementsUpdated.clone(),
          merkleTree == null ? null : merkleTree.copy());
    }
  }

  public SSZIncrementalHasher(
      SSZVisitorHandler<SSZSerializerResult> serializer,
      Function<BytesValue, Hash32> hashFunction, int bytesPerChunk) {
    super(serializer, hashFunction, bytesPerChunk);
  }

  @Override
  public MerkleTrie visitComposite(SSZCompositeType type, Object rawValue,
      ChildVisitor<Object, MerkleTrie> childVisitor) {
    if (rawValue instanceof ObservableComposite) {
      SSZIncrementalTracker tracker = (SSZIncrementalTracker)
          ((ObservableComposite) rawValue).getUpdateListener(
              INCREMENTAL_HASHER_OBSERVER_ID, SSZIncrementalTracker::new);
      if (tracker.merkleTree == null) {
        tracker.merkleTree = super.visitComposite(type, rawValue, childVisitor);
      } else if (!tracker.elementsUpdated.isEmpty()){
        if (type.isList() && ((SSZListType) type).getElementType().isBasicType()) {
          tracker.merkleTree =
              updatePackedTrie((SSZListType) type, rawValue, childVisitor, tracker.merkleTree, tracker.elementsUpdated);
        } else {
          tracker.merkleTree =
              updateNonPackedTrie(type, rawValue, childVisitor, tracker.merkleTree, tracker.elementsUpdated);
        }
      }
      tracker.elementsUpdated.clear();

      return tracker.merkleTree;
    } else {
      return super.visitComposite(type, rawValue, childVisitor);
    }
  }

  private MerkleTrie updateNonPackedTrie(
      SSZCompositeType type,
      Object value,
      BiFunction<Integer, Object, MerkleTrie> childVisitor,
      MerkleTrie merkleTree,
      SortedSet<Integer> elementsUpdated) {

    return updateTrie(
        type,
        value,
        idx -> childVisitor.apply(idx, type.getChild(value, idx)).getFinalRoot(),
        type.getChildrenCount(value),
        merkleTree,
        elementsUpdated);
  }

  private MerkleTrie updatePackedTrie(
      SSZListType type,
      Object value,
      BiFunction<Integer, Object, MerkleTrie> childVisitor,
      MerkleTrie oldTrie,
      SortedSet<Integer> elementsUpdated) {

    int typeSize = type.getElementType().getSize();
    int valsPerChunk = bytesPerChunk / typeSize;

    return updateTrie(
        type,
        value,
        idx -> serializePackedChunk(type, value, idx),
        (type.getChildrenCount(value) - 1) / valsPerChunk + 1,
        oldTrie,
        elementsUpdated.stream().map(i -> i / valsPerChunk).distinct().collect(toList()));
  }

  private MerkleTrie updateTrie(
      SSZCompositeType type,
      Object value,
      Function<Integer, BytesValue> childChunkSupplier,
      int newChunksCount,
      MerkleTrie oldTrie,
      Collection<Integer> chunksUpdated) {

    MerkleTrie newTrie = copyWithSize(oldTrie, newChunksCount);
    int newTrieWidth = newTrie.nodes.length / 2;

    int pos = newTrieWidth;

    List<Integer> elementsToRecalc = new ArrayList<>();
    for (int i: chunksUpdated) {
      if (i < newTrieWidth) {
        elementsToRecalc.add(i);
        if (i < newChunksCount) {
          newTrie.nodes[pos + i] = childChunkSupplier.apply(i);
        }
      }
    }

    int idxShift = 0;
    while (pos > 1) {
      pos /= 2;
      idxShift++;
      int lastIdx = Integer.MAX_VALUE;
      for (int i: elementsToRecalc) {
        int idx = pos + (i >> idxShift);
        if (lastIdx != idx) {
          newTrie.nodes[idx] = hashFunction.apply(
              BytesValue.concat(newTrie.nodes[idx * 2], newTrie.nodes[idx * 2 + 1]));
          lastIdx = idx;
        }
      }
    }
    if (type.isList() && !((SSZListType) type).isVector()) {
      Hash32 mixInLength = hashFunction.apply(BytesValue.concat(
          newTrie.getPureRoot(),
          serializeLength(type.getChildrenCount(value))));
      newTrie.setFinalRoot(mixInLength);
    } else {
      newTrie.setFinalRoot(newTrie.getPureRoot());
    }
    return newTrie;
  }

  private MerkleTrie copyWithSize(MerkleTrie trie, int newChunksCount) {
    int newSize = (int) nextPowerOf2(newChunksCount) * 2;
//    if (newSize == trie.nodes.length) {
//      return new MerkleTrie(Arrays.copyOf(trie.nodes, newSize));
//    } else {
      BytesValue[] oldNodes = trie.nodes;
      BytesValue[] newNodes = new BytesValue[newSize];
      int oldPos = oldNodes.length / 2;
      int newPos = newNodes.length / 2;
      int size = min(newChunksCount, trie.nodes.length / 2);
      int dist = 0;
      while (newPos > 0 ) {
        System.arraycopy(oldNodes, oldPos, newNodes, newPos, size);
        Arrays.fill(newNodes, newPos + size, newPos * 2, getZeroHash(dist));
        oldPos /= 2;
        newPos /= 2;
        size = size == 1 ? 0 : (size - 1) / 2 + 1;
        dist++;
      }

      return new MerkleTrie(newNodes);
//    }
  }

  private BytesValue serializePackedChunk(SSZListType basicListType, Object listValue, int chunkIndex) {
    int typeSize = basicListType.getElementType().getSize();
    int valsPerChunk = bytesPerChunk / typeSize;
    if (valsPerChunk * typeSize != bytesPerChunk) {
      throw new UnsupportedOperationException("");
    }
    int idx = chunkIndex * valsPerChunk;
    int len = Math.min(valsPerChunk, basicListType.getChildrenCount(listValue) - idx);
    BytesValue chunk = serializer.visitList(basicListType, listValue, idx, len)
        .getSerializedBody();
    if (len < valsPerChunk) {
      chunk = BytesValue.concat(chunk, BytesValue.wrap(new byte[bytesPerChunk - chunk.size()]));
    }
    return chunk;
  }
}

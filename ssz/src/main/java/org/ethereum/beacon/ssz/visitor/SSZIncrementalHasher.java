package org.ethereum.beacon.ssz.visitor;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
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
      BiFunction<Integer, Object, MerkleTrie> childVisitor) {
    if (rawValue instanceof ObservableComposite) {
      SSZIncrementalTracker tracker = (SSZIncrementalTracker)
          ((ObservableComposite) rawValue).getUpdateListener(
              INCREMENTAL_HASHER_OBSERVER_ID, SSZIncrementalTracker::new);
      if (tracker.merkleTree == null) {
        tracker.merkleTree = super.visitComposite(type, rawValue, childVisitor);
      } else if (!tracker.elementsUpdated.isEmpty()){
        if (type.isList() && ((SSZListType) type).getElementType().isBasicType()) {
//          tracker.merkleTree =
//              updatePackedTrie(value, childVisitor, tracker.merkleTree, tracker.elementsUpdated);
          // TODO fallback to full recalculation for now
          tracker.merkleTree = super.visitComposite(type, rawValue, childVisitor);
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
    MerkleTrie newTrie = copyWithSize(merkleTree, type.getChildrenCount(value));

    int pos = newTrie.nodes.length / 2;

    for (int i: elementsUpdated) {
      MerkleTrie childHash = childVisitor.apply(i, type.getChild(value, i));
      newTrie.nodes[pos + i] = childHash.getFinalRoot();
    }

    int idxShift = 0;
    while (pos > 1) {
      pos /= 2;
      idxShift++;
      int lastIdx = Integer.MAX_VALUE;
      for (int i: elementsUpdated) {
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

  private MerkleTrie updatePackedTrie(
      SSZCompositeType type,
      Object value,
      Function<Long, MerkleTrie> childVisitor,
      MerkleTrie merkleTree,
      SortedSet<Integer> elementsUpdated) {

    throw new UnsupportedOperationException();
  }
  private MerkleTrie copyWithSize(MerkleTrie trie, int newChunksCount) {
    int newSize = (int) nextPowerOf2(newChunksCount) * 2;
    if (newSize == trie.nodes.length) {
      return new MerkleTrie(Arrays.copyOf(trie.nodes, newSize));
    } else if (newSize > trie.nodes.length) {
      BytesValue[] oldNodes = trie.nodes;
      BytesValue[] newNodes = new BytesValue[newSize];
      int oldPos = oldNodes.length / 2;
      int newPos = newNodes.length / 2;
      int dist = 0;
      while (newPos > 0) {
        System.arraycopy(oldNodes, oldPos, newNodes, newPos, oldPos);
        Arrays.fill(newNodes, newPos + oldPos, newPos * 2, getZeroHash(dist));
        oldPos /= 2;
        newPos /= 2;
        dist++;
      }

      return new MerkleTrie(newNodes);
    } else {
      throw new UnsupportedOperationException("Reducing trie size not implemented yet");
    }
  }
}

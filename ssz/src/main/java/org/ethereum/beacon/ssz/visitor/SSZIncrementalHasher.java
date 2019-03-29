package org.ethereum.beacon.ssz.visitor;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Function;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer.SSZSerializerResult;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SSZIncrementalHasher extends SSZSimpleHasher {

  static class SSZIncrementalTracker {
    SortedSet<Integer> elementsUpdated;
    MerkleTrie merkleTree;
  }

  public SSZIncrementalHasher(
      SSZVisitorHandler<SSZSerializerResult> serializer,
      Function<BytesValue, Hash32> hashFunction, int bytesPerChunk) {
    super(serializer, hashFunction, bytesPerChunk);
  }

  @Override
  public MerkleTrie visitComposite(SSZCompositeValue value, Function<Long, MerkleTrie> childVisitor) {
    if (value.getRawValue() instanceof Incremental) {
      SSZIncrementalTracker tracker = ((Incremental) value.getRawValue()).getTracker();
      if (tracker.merkleTree == null) {
        tracker.merkleTree = super.visitComposite(value, childVisitor);
      } else if (!tracker.elementsUpdated.isEmpty()){
        if (value.getCompositeType().isBasicElementType()) {
//          tracker.merkleTree =
//              updatePackedTrie(value, childVisitor, tracker.merkleTree, tracker.elementsUpdated);
          // TODO fallback to full recalculation for now
          tracker.merkleTree = super.visitComposite(value, childVisitor);
        } else {
          tracker.merkleTree =
              updateNonPackedTrie(value, childVisitor, tracker.merkleTree, tracker.elementsUpdated);
        }
      }
      tracker.elementsUpdated.clear();
      return tracker.merkleTree;
    } else {
      return super.visitComposite(value, childVisitor);
    }
  }

  private MerkleTrie updateNonPackedTrie(
      SSZCompositeValue value,
      Function<Long, MerkleTrie> childVisitor,
      MerkleTrie merkleTree,
      SortedSet<Integer> elementsUpdated) {
    MerkleTrie newTrie = copyWithSize(merkleTree, (int) value.getChildCount());

    int pos = newTrie.nodes.length / 2;

    for (int i: elementsUpdated) {
      MerkleTrie childHash = childVisitor.apply((long) i);
      newTrie.nodes[pos + i] = childHash.getFinalRoot();
    }

    int idxShift = 0;
    while (pos > 1) {
      pos /= 2;
      idxShift++;
      int lastIdx = Integer.MAX_VALUE;
      for (int i: elementsUpdated) {
        int idx = pos + i >> idxShift;
        if (lastIdx != idx) {
          newTrie.nodes[idx] = hashFunction.apply(
              BytesValue.concat(newTrie.nodes[idx * 2], newTrie.nodes[idx * 2 + 1]));
          lastIdx = idx;
        }
      }
    }
    if (value.getCompositeType().isVariableSize()) {
      Hash32 mixInLength = hashFunction
          .apply(BytesValue.concat(newTrie.getFinalRoot(), serializeLength(value.getChildCount())));
      newTrie.setFinalRoot(mixInLength);
    }
    return newTrie;
  }

  private MerkleTrie updatePackedTrie(
      SSZCompositeValue value,
      Function<Long, MerkleTrie> childVisitor,
      MerkleTrie merkleTree,
      SortedSet<Integer> elementsUpdated) {

    throw new UnsupportedOperationException();
  }

  private MerkleTrie copyWithSize(MerkleTrie trie, int newChunksCount) {
    int newSize = (int) nextPowerOf2(newChunksCount);
    MerkleTrie copy = new MerkleTrie(Arrays.copyOf(trie.nodes, newSize));
    if (copy.nodes.length > trie.nodes.length) {
      for (int i = newChunksCount; i < newSize; i++) {
        copy.nodes[i] = Hash32.ZERO;
      }
    }
    return copy;
  }
}

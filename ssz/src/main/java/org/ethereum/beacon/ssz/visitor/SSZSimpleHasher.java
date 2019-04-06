package org.ethereum.beacon.ssz.visitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.visitor.SSZSimpleHasher.MerkleTrie;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer.SSZSerializerResult;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;

public class SSZSimpleHasher implements SSZVisitor<MerkleTrie, Object> {

  public static class MerkleTrie {
    final BytesValue[] nodes;

    public MerkleTrie(BytesValue[] nodes) {
      this.nodes = nodes;
    }

    public Hash32 getPureRoot() {
      return Hash32.wrap(Bytes32.leftPad(nodes[1]));
    }

    public Hash32 getFinalRoot() {
      return Hash32.wrap(Bytes32.leftPad(nodes[0]));
    }

    public void setFinalRoot(Hash32 mixedInLengthHash) {
      nodes[0] = mixedInLengthHash;
    }
  }

  final SSZVisitorHandler<SSZSimpleSerializer.SSZSerializerResult> serializer;
  final Function<BytesValue, Hash32> hashFunction;
  final int bytesPerChunk;

  public SSZSimpleHasher(
      SSZVisitorHandler<SSZSerializerResult> serializer,
      Function<BytesValue, Hash32> hashFunction, int bytesPerChunk) {
    this.serializer = serializer;
    this.hashFunction = hashFunction;
    this.bytesPerChunk = bytesPerChunk;
  }

  @Override
  public MerkleTrie visitBasicValue(SSZBasicType descriptor, Object value) {
    SSZSimpleSerializer.SSZSerializerResult sszSerializerResult = serializer.visitAny(descriptor, value);
    return merkleize(pack(sszSerializerResult.serializedBody));
  }

  @Override
  public MerkleTrie visitComposite(SSZCompositeType type, Object rawValue,
      BiFunction<Integer, Object, MerkleTrie> childVisitor) {
    MerkleTrie merkleize;
    if (type.isList() && ((SSZListType) type).getElementType().isBasicType()) {
      SSZSimpleSerializer.SSZSerializerResult sszSerializerResult = serializer.visitAny(type, rawValue);

      merkleize = merkleize(pack(sszSerializerResult.serializedBody));
    } else {
      List<Hash32> childHashes = new ArrayList<>();
      for (int i = 0; i < type.getChildrenCount(rawValue); i++) {
        childHashes.add(childVisitor.apply(i, type.getChild(rawValue, i)).getFinalRoot());
      }
      merkleize = merkleize(childHashes);
    }
    if (type.isVariableSize()) {
      Hash32 mixInLength = hashFunction.apply(BytesValue.concat(
          merkleize.getPureRoot(),
          serializeLength(type.getChildrenCount(rawValue))));
      merkleize.setFinalRoot(mixInLength);
    }
    return merkleize;
  }

  protected List<BytesValue> pack(BytesValue value) {
    List<BytesValue> ret = new ArrayList<>();
    int i = 0;
    while (i + bytesPerChunk <= value.size()) {
      ret.add(value.slice(i, bytesPerChunk));
      i += bytesPerChunk;
    }
    if (value.size() % bytesPerChunk != 0) {
      BytesValue last = value.slice(i, value.size() - i);
      BytesValue lastPadded = BytesValue.concat(
          last, BytesValue.wrap(new byte[bytesPerChunk - value.size() % bytesPerChunk]));
      ret.add(lastPadded);
    }
    return ret;
  }

  public MerkleTrie merkleize(List<? extends BytesValue> chunks) {
    int chunksCount = (int) nextPowerOf2(chunks.size());
    BytesValue[] nodes = new BytesValue[chunksCount * 2];

    // TODO optimize: no need to recalc zero hashes on upper trie levels, e.g. hash(zeroHash + zeroHash)
    for (int i = 0; i < chunksCount; i++) {
      nodes[i + chunksCount] = i < chunks.size() ? chunks.get(i) : Bytes32.ZERO;
    }

    for (int i = chunksCount - 1; i > 0; i--) {
      nodes[i] = hashFunction.apply(BytesValue.concat(nodes[i * 2], nodes[i * 2 + 1]));
    }
    nodes[0] = nodes[1];
    BytesValue[] trie = new BytesValue[chunksCount];
    System.arraycopy(nodes, 0, trie, 0, chunksCount);
    return new MerkleTrie(trie);
  }

  protected long nextPowerOf2(int x) {
    if (x <= 1) {
      return 1;
    } else {
      return Long.highestOneBit(x - 1) << 1;
    }
  }

  BytesValue serializeLength(long len) {
    return BytesValue.concat(BytesValues.ofUnsignedIntLittleEndian(len), BytesValue.wrap(new byte[32 - 4]));
  }
}

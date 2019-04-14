package org.ethereum.beacon.ssz.visitor;

import static tech.pegasys.artemis.util.bytes.BytesValue.concat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.visitor.SSZSimpleSerializer.SSZSerializerResult;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;

public class SSZSimpleHasher implements SSZVisitor<MerkleTrie, Object> {

  private final Hash32[] zeroHashes = new Hash32[32];
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
    MerkleTrie merkle;
    List<BytesValue> chunks = new ArrayList<>();
    try {
    if (type.getChildrenCount(rawValue) == 0) {
      // empty chunk list
    } else if (type.isList() && ((SSZListType) type).getElementType().isBasicType()) {
      SSZSimpleSerializer.SSZSerializerResult sszSerializerResult = serializer.visitAny(type, rawValue);

      chunks = pack(sszSerializerResult.serializedBody);
    } else {
      for (int i = 0; i < type.getChildrenCount(rawValue); i++) {
        chunks.add(childVisitor.apply(i, type.getChild(rawValue, i)).getFinalRoot());
      }
    }
    } catch (Exception ex) {
      throw ex;
    }
    merkle = merkleize(chunks);
    if (type.isList() && !((SSZListType) type).isVector()) {
      Hash32 mixInLength = hashFunction.apply(concat(
          merkle.getPureRoot(),
          serializeLength(type.getChildrenCount(rawValue))));
      merkle.setFinalRoot(mixInLength);
    }
    return merkle;
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
      BytesValue lastPadded = concat(
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
      nodes[i] = hashFunction.apply(concat(nodes[i * 2], nodes[i * 2 + 1]));
    }

    nodes[0] = nodes[1];
    return new MerkleTrie(nodes);
  }

  protected long nextPowerOf2(int x) {
    if (x <= 1) {
      return 1;
    } else {
      return Long.highestOneBit(x - 1) << 1;
    }
  }

  protected Hash32 getZeroHash(int distanceFromBottom) {
    if (zeroHashes[distanceFromBottom] == null) {
      if (distanceFromBottom == 0) {
        zeroHashes[0] = Hash32.ZERO;
      } else {
        Hash32 lowerZeroHash = getZeroHash(distanceFromBottom - 1);
        zeroHashes[distanceFromBottom] = hashFunction
            .apply(concat(lowerZeroHash, lowerZeroHash));
      }
    }
    return zeroHashes[distanceFromBottom];
  }

  BytesValue serializeLength(long len) {
    return concat(BytesValues.ofUnsignedIntLittleEndian(len), BytesValue.wrap(new byte[32 - 4]));
  }
}

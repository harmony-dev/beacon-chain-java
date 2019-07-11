package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.ssz.access.SSZListAccessor;
import org.ethereum.beacon.ssz.access.SSZUnionAccessor.UnionInstanceAccessor;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZBitListType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZUnionType;
import org.ethereum.beacon.ssz.visitor.SosSerializer.SerializerResult;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.ethereum.beacon.ssz.type.SSZType.Type.BASIC;
import static org.ethereum.beacon.ssz.type.SSZType.Type.LIST;
import static org.ethereum.beacon.ssz.type.SSZType.Type.VECTOR;
import static tech.pegasys.artemis.util.bytes.BytesValue.concat;

public class SSZSimpleHasher implements SSZVisitor<MerkleTrie, Object> {

  private final Hash32[] zeroHashes = new Hash32[32];
  final SSZVisitorHandler<SerializerResult> serializer;
  final Function<BytesValue, Hash32> hashFunction;
  final int bytesPerChunk;

  public SSZSimpleHasher(
      SSZVisitorHandler<SerializerResult> serializer,
      Function<BytesValue, Hash32> hashFunction, int bytesPerChunk) {
    this.serializer = serializer;
    this.hashFunction = hashFunction;
    this.bytesPerChunk = bytesPerChunk;
  }

  @Override
  public MerkleTrie visitBasicValue(SSZBasicType descriptor, Object value) {
    SerializerResult sszSerializerResult = serializer.visitAny(descriptor, value);
    return merkleize(pack(sszSerializerResult.getSerializedBody()));
  }

  @Override
  public MerkleTrie visitUnion(SSZUnionType type, Object param,
      ChildVisitor<Object, MerkleTrie> childVisitor) {
    UnionInstanceAccessor unionInstanceAccessor = type.getAccessor().getInstanceAccessor(type.getTypeDescriptor());
    int typeIndex = unionInstanceAccessor.getTypeIndex(param);
    List<BytesValue> chunks;
    if (type.isNullable() && typeIndex == 0) {
      chunks = emptyList();
    } else {
      Object value = unionInstanceAccessor.getChildValue(param, typeIndex);
      chunks = singletonList(childVisitor.apply(typeIndex, value).getFinalRoot());
    }
    MerkleTrie merkle = merkleize(chunks);
    Hash32 mixInType = hashFunction.apply(concat(merkle.getPureRoot(), serializeLength(typeIndex)));
    merkle.setFinalRoot(mixInType);
    return merkle;
  }

  @Override
  public MerkleTrie visitComposite(SSZCompositeType type, Object rawValue,
      ChildVisitor<Object, MerkleTrie> childVisitor) {
    MerkleTrie merkle;
    List<BytesValue> chunks = new ArrayList<>();
    if (type.getChildrenCount(rawValue) == 0) {
      // empty chunk list
    } else if ((type.getType() == LIST || type.getType() == VECTOR)
        && ((SSZListType) type).getElementType().getType() == BASIC) {
      SerializerResult sszSerializerResult = serializer.visitAny(type, rawValue);
      BytesValue serialization;
      // Strip size bit in Bitlist
      if (type.getType() == LIST && type instanceof SSZBitListType) {
        serialization = removeBitListSize(rawValue, sszSerializerResult.getSerializedBody());
      } else {
        serialization = sszSerializerResult.getSerializedBody();
      }
      chunks = pack(serialization);
    } else {
      for (int i = 0; i < type.getChildrenCount(rawValue); i++) {
        chunks.add(childVisitor.apply(i, type.getChild(rawValue, i)).getFinalRoot());
      }
    }
    merkle = merkleize(chunks);
    if (type.getType() == LIST) {
      SSZListAccessor listAccessor =
          (SSZListAccessor) type.getAccessor().getInstanceAccessor(type.getTypeDescriptor());
      int elementCount;
      if (type instanceof SSZBitListType) {
        elementCount = ((Bitlist) rawValue).size();
      } else {
        elementCount = listAccessor.getChildrenCount(rawValue);
      }
      Hash32 mixInLength =
          hashFunction.apply(concat(merkle.getPureRoot(), serializeLength(elementCount)));
      merkle.setFinalRoot(mixInLength);
    }
    return merkle;
  }

  private BytesValue removeBitListSize(Object value, BytesValue bitlist) {
    MutableBytesValue encoded = bitlist.mutableCopy();
    Bitlist obj = (Bitlist) value;
    encoded.setBit(obj.size(), false);
    return encoded.copy();
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

    int len = (chunks.size() - 1) / 2 + 1;
    int pos = chunksCount / 2;
    int level = 1;
    while (pos > 0) {
      for (int i = 0; i < len; i++) {
        nodes[pos + i] = hashFunction.apply(concat(nodes[(pos + i) * 2], nodes[(pos + i) * 2 + 1]));
      }
      for (int i = len; i < pos; i++) {
        nodes[pos + i] = getZeroHash(level);
      }
      len = (len - 1) / 2 + 1;
      pos /= 2;
      level++;
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

  public Hash32 getZeroHash(int distanceFromBottom) {
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

  static BytesValue serializeLength(long len) {
    return concat(BytesValues.ofUnsignedIntLittleEndian(len), BytesValue.wrap(new byte[Hash32.SIZE - Integer.BYTES]));
  }
}

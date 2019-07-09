package org.ethereum.beacon.core.types;

import com.google.common.base.Objects;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.list.AbstractListAccessor;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.DelegatingBytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;

@SSZSerializable(listAccessor = Bitvector.BitVectorAccessor.class)
public class Bitvector extends DelegatingBytesValue {
  private final int size;

  public static Bitvector EMPTY = of(0, BytesValue.EMPTY);

  public static Bitvector of(int size, BytesValue bytes) {
    return new Bitvector(size, bytes);
  }

  public static Bitvector of(int size, long bytes) {
    UInt64 blank = UInt64.valueOf(bytes);
    if (blank.getUsedBitCount() > size) {
      throw new IndexOutOfBoundsException(String.format("Input data %s exceeds Bitvector size of %s", bytes, size));
    }

    int neededBytes = (size + 7) / 8;
    return new Bitvector(size, blank.toBytesValue().slice(0, neededBytes));
  }

  Bitvector(int size, BytesValue bytes) {
    super(checkSize(bytes, size));
    this.size = size;
  }

  private static BytesValue checkSize(BytesValue input, int size) {
    int neededBytes = (size + 7) / 8;
    if (neededBytes != input.size()) {
      throw new IllegalArgumentException(String.format("An attempt to initialize Bitvector with size %s using value %s with another size", size, input));
    }

    return input;
  }

  public Bitvector setBit(int bitIndex, int bit) {
    assert bit == 0 || bit == 1;
    return setBit(bitIndex, bit == 1);
  }

  public Bitvector setBit(int bitIndex, boolean bit) {
    if (bitIndex >= size) {
      throw new IndexOutOfBoundsException(String.format("An attempt to set bit #%s for Bitvector with size %s", bitIndex, size));
    }
    MutableBytesValue mutableCopy = mutableCopy();
    mutableCopy.setBit(bitIndex, bit);
    return new Bitvector(size, mutableCopy);
  }

  public List<Integer> getBits() {
    List<Integer> ret = new ArrayList<>();
    for (int i = 0; i < size(); i++) {
      if (getBit(i)) {
        ret.add(i);
      }
    }
    return ret;
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder("0b");
    for (int i = 0; i < byteSize(); i++) {
      ret.append(toBinaryString(get(i)));
    }
    return ret.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Bitvector bitVector = (Bitvector) o;
    return size == bitVector.size && wrapped.equals(((Bitvector) o).wrapped);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), size);
  }

  private String toBinaryString(byte b) {
    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < 8; i++) {
      ret.append((b >> (7 - i)) & 1);
    }
    return ret.toString();
  }

  public int size() {
    return size;
  }

  public int byteSize() {
    return (size + 7) / 8;
  }

  public Bitvector shl(int i) {
    List<Integer> oldSet = getBits();
    MutableBytesValue mutableBytes = MutableBytesValue.create(byteSize());
    for (Integer index: oldSet) {
      if ((index + 1) == size) { // skip last one
        continue;
      }
      mutableBytes.setBit(index + 1, true);
    }

    return new Bitvector(size, mutableBytes.copy());
  }

  public long getValue() {
    return UInt64.fromBytesLittleEndian(Bytes8.leftPad(wrapped)).getValue();
  }

  public static class BitVectorAccessor extends AbstractListAccessor {

    @Override
    public int getChildrenCount(Object value) {
      return ((Bitvector) value).byteSize();
    }

    @Override
    public int fromAtomicSize(int claimedSize) {
      return claimedSize == SSZType.VARIABLE_SIZE ? claimedSize : (claimedSize + 7) / 8; // bits to bytes
    }

    @Override
    public Object getChildValue(Object value, int idx) {
      return (byte) ((Bitvector) value).wrapped.get(idx);
    }

    @Override
    public SSZField getListElementType(SSZField listTypeDescriptor) {
      return new SSZField(byte.class);
    }



    @Override
    public ListInstanceBuilder createInstanceBuilder(SSZType type) {
      return new SimpleInstanceBuilder() {
        @Override
        protected Object buildImpl(List<Object> children) {
          MutableBytesValue blank = MutableBytesValue.create(children.size());
          for (int i = 0; i < children.size(); i++) {
            blank.set(i, ((Integer) children.get(i)).byteValue());
          }
          return new Bitvector(((SSZListType) type).getAtomicSize(), blank.copy());
        }
      };
    }

    @Override
    public boolean isSupported(SSZField field) {
      return Bitvector.class.isAssignableFrom(field.getRawClass());
    }
  }
}

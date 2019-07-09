package org.ethereum.beacon.core.types;

import com.google.common.base.Objects;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.list.AbstractListAccessor;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;
import tech.pegasys.artemis.util.bytes.DelegatingBytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;

@SSZSerializable(listAccessor = Bitlist.BitListAccessor.class)
public class Bitlist extends DelegatingBytesValue {
  private final int size;
  private final long maxSize;

  Bitlist(int size, BytesValue bytes, long maxSize) {
    super(checkSize(bytes, size));
    this.size = size;
    this.maxSize = maxSize;
  }

  /** Assumes that size info is encoded in bytes\ */
  public static Bitlist of(BytesValue bytes, long maxSize) {
    int size = (bytes.size() - 1) * 8;
    byte lastByte = bytes.get(bytes.size() - 1);
    int addon = 0;
    for (int i = 0; i < 8; ++i) {
      if (((lastByte >> i) & 1) == 1) {
        addon = i;
      }
    }
    final BytesValue finalBlank;
    if (addon == 0) {
      finalBlank = bytes.slice(0, bytes.size() - 1); // last byte was needed only for a size
    } else {
      size = size + addon;
      MutableBytesValue mutableBytes = bytes.mutableCopy();
      mutableBytes.setBit(size, false);
      finalBlank = mutableBytes.copy();
    }

    return new Bitlist(size, finalBlank, maxSize);
  }

  public static Bitlist of(int size, BytesValue bytes, long maxSize) {
    return new Bitlist(size, bytes, maxSize);
  }

  public static Bitlist of(int maxSize) {
    return new Bitlist(0, BytesValue.EMPTY, maxSize);
  }

  public static Bitlist of(int size, long bytes, long maxSize) {
    UInt64 blank = UInt64.valueOf(bytes);
    if (blank.getUsedBitCount() > size) {
      throw new IndexOutOfBoundsException(
          String.format("Input data %s exceeds Bitlist size of %s", bytes, size));
    }

    int neededBytes = (size + 7) / 8;
    return new Bitlist(size, blank.toBytesValue().slice(0, neededBytes), maxSize);
  }

  private static BytesValue checkSize(BytesValue input, int size) {
    int neededBytes = (size + 7) / 8;
    if (neededBytes != input.size()) {
      throw new IllegalArgumentException(
          String.format(
              "An attempt to initialize Bitlist with size %s using value %s with another size",
              size, input));
    }

    return input;
  }

  public Bitlist setBit(int bitIndex, boolean bit) {
    if (bitIndex > maxSize) {
      throw new IndexOutOfBoundsException(
          String.format("An attempt to set bit #%s for Bitlist with maxSize %s", bitIndex, size));
    }
    MutableBytesValue mutableCopy = mutableCopy();
    mutableCopy.setBit(bitIndex, bit);
    return new Bitlist(size, mutableCopy, maxSize);
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

  public Bitlist and(Bitlist other) {
    return Bitlist.of(Math.max(size, other.size), BytesValues.and(this, other), maxSize);
  }

  public Bitlist or(Bitlist other) {
    return Bitlist.of(size, BytesValues.or(this, other), maxSize);
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
    Bitlist bitList = (Bitlist) o;
    return size == bitList.size
        && wrapped.equals(((Bitlist) o).wrapped)
        && maxSize == bitList.maxSize;
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
    // bits to bytes + size bit
    return (size + 8) / 8;
  }

  public static class BitListAccessor extends AbstractListAccessor {

    @Override
    public int getChildrenCount(Object value) {
      return ((Bitlist) value).byteSize();
    }

    @Override
    public long fromAtomicSize(long elementSize) {
      return elementSize == SSZType.VARIABLE_SIZE
          ? elementSize
          : (elementSize + 8) / 8; // bits to bytes + size bit
    }

    @Override
    public int getAtomicChildrenCount(Object value) {
      return ((Bitlist) value).size;
    }

    @Override
    public Object getChildValue(Object value, int idx) {
      Bitlist bitlist = ((Bitlist) value);
      if ((idx + 1) == bitlist.byteSize()) {
        byte withoutSize = idx < ((bitlist.size() + 7) / 8) ? bitlist.wrapped.get(idx) : 0;
        int bitNumber = bitlist.size() % 8;
        return withoutSize | (1 << bitNumber); // add size bit
      } else {
        return bitlist.wrapped.get(idx);
      }
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

          return Bitlist.of(blank, ((SSZListType) type).getMaxAtomicSize());
        }
      };
    }

    @Override
    public BytesValue removeListSize(Object value, BytesValue serialization) {
      MutableBytesValue encoded = serialization.mutableCopy();
      Bitlist obj = (Bitlist) value;
      encoded.setBit(obj.size, false);
      return encoded.copy();
    }

    @Override
    public boolean isSupported(SSZField field) {
      return Bitlist.class.isAssignableFrom(field.getRawClass());
    }
  }
}

package tech.pegasys.artemis.util.collections;

import com.google.common.base.Objects;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;
import tech.pegasys.artemis.util.bytes.DelegatingBytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;

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
      throw new IllegalArgumentException(
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
              "An attempt to initialize Bitlist/Bitvector with size %s using value %s with another size",
              size, input));
    }

    return input;
  }

  public Bitlist setBit(int bitIndex, int bit) {
    assert bit == 0 || bit == 1;
    return setBit(bitIndex, bit == 1);
  }

  void verifyBitModification(int bitIndex) {
    if (bitIndex > maxSize()) {
      throw new IndexOutOfBoundsException(
          String.format("An attempt to set bit #%s for Bitlist with maxSize %s", bitIndex, size));
    }
  }

  public Bitlist setBit(int bitIndex, boolean bit) {
    verifyBitModification(bitIndex);
    MutableBytesValue mutableCopy = mutableCopy();
    mutableCopy.setBit(bitIndex, bit);
    return new Bitlist(size, mutableCopy, maxSize());
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
    return Bitlist.of(
        Math.max(size, other.size), BytesValues.and(this.wrapped, other.wrapped), maxSize());
  }

  public Bitlist or(Bitlist other) {
    return Bitlist.of(size, BytesValues.or(this.wrapped, other.wrapped), maxSize());
  }

  public Bitlist shl(int i) {
    List<Integer> oldSet = getBits();
    MutableBytesValue mutableBytes = MutableBytesValue.create(byteSize());
    for (Integer index : oldSet) {
      if ((index + 1) == size) { // skip last one
        continue;
      }
      mutableBytes.setBit(index + 1, true);
    }

    return new Bitlist(size, mutableBytes.copy(), maxSize());
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder("0b");
    for (int i = 0; i < rawByteSize(); i++) {
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
        && maxSize() == bitList.maxSize();
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

  public long maxSize() {
    return maxSize;
  }

  int rawByteSize() {
    // bits to bytes
    return (size + 7) / 8;
  }

  public int byteSize() {
    // bits to bytes + size bit
    return (size + 8) / 8;
  }

  BytesValue getWrapped() {
    return wrapped;
  }

  public long getValue() {
    assert wrapped.size() <= 8;
    return UInt64.fromBytesLittleEndian(Bytes8.rightPad(wrapped)).getValue();
  }
}

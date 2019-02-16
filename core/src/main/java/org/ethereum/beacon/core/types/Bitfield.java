package org.ethereum.beacon.core.types;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;
import tech.pegasys.artemis.util.bytes.DelegatingBytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;

@SSZSerializable(serializeAs = BytesValue.class)
public class Bitfield extends DelegatingBytesValue {

  public static final Bitfield EMPTY = new Bitfield(BytesValue.of());

  public static Bitfield of(BytesValue bytes) {
    return new Bitfield(bytes);
  }

  public Bitfield(BytesValue bytes) {
    super(bytes);
  }

  public Bitfield setBit(int bitIndex, boolean bit) {
    MutableBytesValue mutableCopy = mutableCopy();
    mutableCopy.setBit(bitIndex, bit);
    return new Bitfield(mutableCopy);
  }

  public Bitfield and(Bitfield other) {
    return Bitfield.of(BytesValues.and(this, other));
  }

  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder("0b");
    for (int i = 0; i < size(); i++) {
      ret.append(toBinaryString(get(i)));
    }
    return ret.toString();
  }

  private String toBinaryString(byte b) {
    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < 8; i++) {
      ret.append((b >> (7 - i)) & 1);
    }
    return ret.toString();
  }
}

package org.ethereum.beacon.core.types;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.BytesValue;
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
}

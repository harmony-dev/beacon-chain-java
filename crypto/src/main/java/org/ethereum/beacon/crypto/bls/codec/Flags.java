package org.ethereum.beacon.crypto.bls.codec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;

public class Flags {

  static final int A = 1;
  static final int B = 2;
  static final int C = 4;

  static final int SIGN = A;
  static final int INFINITY = B;

  private final int bits;

  private Flags(int bits) {
    this.bits = bits;
  }

  static Flags create(boolean infinity, int sign) {
    if (infinity) {
      return new Flags(C | INFINITY);
    } else if (sign > 0) {
      return new Flags(C | SIGN);
    } else {
      return new Flags(C);
    }
  }

  static Flags empty() {
    return new Flags(0);
  }

  @VisibleForTesting
  public static Flags read(byte[] stream) {
    return read(stream, 0);
  }

  static Flags read(byte[] stream, int idx) {
    assert stream.length > idx;
    return new Flags((stream[idx] >> 5) & 0x7);
  }

  static byte[] erase(byte[] stream) {
    assert stream.length > 0;

    byte[] cloned = stream.clone();
    cloned[0] = (byte) (cloned[0] & 0x1F);
    return cloned;
  }

  byte[] write(byte[] stream) {
    assert stream.length > 0;

    byte[] cloned = stream.clone();
    int highestByte = cloned[0] | (bits << 5);
    cloned[0] = (byte) (highestByte & 0xFF);
    return cloned;
  }

  boolean isZero() {
    return bits == 0;
  }

  public int test(int flag) {
    return (bits & flag) > 0 ? 1 : 0;
  }

  @VisibleForTesting
  public boolean isSignSet() {
    return test(SIGN) > 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Flags flags = (Flags) o;
    return bits == flags.bits;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("c", test(C))
        .add("inf", test(INFINITY))
        .add("sign", test(SIGN))
        .toString();
  }
}

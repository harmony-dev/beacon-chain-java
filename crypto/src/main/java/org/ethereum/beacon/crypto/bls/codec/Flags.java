package org.ethereum.beacon.crypto.bls.codec;

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
    int bits = C;
    if (infinity) {
      bits |= INFINITY;
    }
    if (sign > 0) {
      bits |= SIGN;
    }

    return new Flags(bits);
  }

  static Flags empty() {
    return new Flags(0);
  }

  static Flags read(byte[] stream) {
    return read(stream, 0);
  }

  static Flags read(byte[] stream, int idx) {
    assert stream.length > idx;
    return new Flags(stream[idx] >> 5);
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

  int test(int flag) {
    return (bits & flag) > 0 ? 1 : 0;
  }
}

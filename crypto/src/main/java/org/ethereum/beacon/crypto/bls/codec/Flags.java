package org.ethereum.beacon.crypto.bls.codec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import org.apache.milagro.amcl.BLS381.BIG;
import org.ethereum.beacon.crypto.bls.milagro.BIGs;

/**
 * A class to work with flags which are a part of representation format of elliptic curve points.
 *
 * <p>These flags are stored in three highest bits of {@code x} coordinate of the point in a
 * following order: {@code {c_flag | b_flag | a_flag}} where {@code c_flag} lands to the highest bit
 * of {@code x}.
 *
 * @see PointData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md#point-representations">https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md#point-representations</a>
 */
public class Flags {

  /**
   * A bit that holds a sign of {@code y} coordinate.
   *
   * <p>Check {@link BIGs#getSign(BIG, BIG)} to get the idea of what sign is.
   */
  static final int A = 1;
  /**
   * A bit that is set to {@code 1} when point is a point at infinity, otherwise, it must be set to
   * {@code 0}.
   */
  static final int B = 2;
  /**
   * A bit signaling that point is encoded in a compressed format. In our case always set to {@code
   * 1}.
   */
  static final int C = 4;

  static final int SIGN = A;
  static final int INFINITY = B;

  private final int bits;

  private Flags(int bits) {
    this.bits = bits;
  }

  /**
   * Creates an instance from the infinity and the sign.
   *
   * @param infinity whether point is a point at infinity.
   * @param sign a sign.
   * @return the instance of flags.
   */
  static Flags create(boolean infinity, int sign) {
    if (infinity) {
      return new Flags(C | INFINITY);
    } else if (sign > 0) {
      return new Flags(C | SIGN);
    } else {
      return new Flags(C);
    }
  }

  /**
   * Creates flags instance with all flags set to {@code 0}.
   *
   * @return the instance.
   */
  static Flags empty() {
    return new Flags(0);
  }

  @VisibleForTesting
  public static Flags read(byte value) {
    return new Flags((value >> 5) & 0x7);
  }

  static byte erase(byte value) {
    return (byte) (value & 0x1F);
  }

  byte write(byte value) {
    return  (byte) ((value | (bits << 5)) & 0xFF);
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

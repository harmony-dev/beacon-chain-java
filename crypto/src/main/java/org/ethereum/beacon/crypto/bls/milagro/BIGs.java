package org.ethereum.beacon.crypto.bls.milagro;

import java.math.BigInteger;
import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.FP;
import org.bouncycastle.util.Arrays;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public abstract class BIGs {
  private BIGs() {}

  private static final boolean BIG_ENDIAN = true;

  public static byte[] toByteArray(BIG big, boolean bigEndian) {
    byte[] bytes = new byte[BIG.MODBYTES];
    big.toBytes(bytes);
    return fixEndiannes(bytes, bigEndian);
  }

  public static byte[] toByteArray(BIG big) {
    return toByteArray(big, BIG_ENDIAN);
  }

  public static BytesValue toBytes(BIG big) {
    return BytesValue.wrap(toByteArray(big, BIG_ENDIAN));
  }

  public static BIG fromByteArray(byte[] bytes, boolean bigEndian) {
    assert bytes.length <= BIG.MODBYTES;

    byte[] fixed = fixEndiannes(bytes, bigEndian);
    byte[] prepended = prependWithZeros(fixed, BIG.MODBYTES);

    return BIG.fromBytes(prepended);
  }

  public static BIG fromByteArray(byte[] bytes) {
    return fromByteArray(bytes, BIG_ENDIAN);
  }

  public static BIG fromBytes(BytesValue bytes) {
    return fromByteArray(bytes.getArrayUnsafe(), BIG_ENDIAN);
  }

  public static BIG fromBigInteger(BigInteger value) {
    return fromByteArray(value.toByteArray(), true);
  }

  public static int getSign(BIG value, BIG modulus) {
    return BIG.sqr(value).div(modulus).bit(0);
  }

  public static BIG neg(BIG value) {
    FP fp = new FP(value);
    fp.neg();
    return fp.redc();
  }

  private static byte[] fixEndiannes(byte[] bytes, boolean bigEndian) {
    return bigEndian ? bytes : Arrays.reverse(bytes);
  }

  private static byte[] prependWithZeros(byte[] bytes, int targetLen) {
    if (bytes.length < targetLen) {
      byte[] prepended = new byte[targetLen];
      System.arraycopy(bytes, 0, prepended, targetLen - bytes.length, bytes.length);
      return prepended;
    } else {
      return bytes;
    }
  }
}

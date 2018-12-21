package org.ethereum.beacon.crypto.bls.bc;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

public interface BCParameters {

  BigInteger Q =
      new BigInteger(
          "1a0111ea397fe69a4b1ba7b6434bacd764774b84f38512bf6730d2a0f6b0f6241eabfffeb153ffffb9feffffffffaaab",
          16);

  BigInteger G_ORDER =
      new BigInteger("73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001", 16);

  BigInteger A = BigInteger.ZERO;
  BigInteger B = BigInteger.valueOf(4);

  int Q_BYTE_LENGTH = Q.bitLength() / 8 + 1;

  abstract class G1 {
    private G1() {}

    public static final BigInteger COFACTOR =
        new BigInteger("396c8c005555e1568c00aaab0000aaab", 16);

    public static final ECCurve CURVE;
    public static final ECPoint G;

    static {
      CURVE = new ECCurve.Fp(Q, A, B, G_ORDER, COFACTOR);
      G =
          CURVE.createPoint(
              new BigInteger(
                  "17f1d3a73197d7942695638c4fa9ac0fc3688c4f9774b905a14e3a3f171bac586c55e83ff97a1aeffb3af00adb22c6bb",
                  16),
              new BigInteger(
                  "8b3f481e3aaa0f1a09e30ed741d8ae4fcf5e095d5d00af600db18cb2c04b3edd03cc744a2888ae40caa232946c5e7e1",
                  16));
    }
  }

  abstract class G2 {
    private G2() {}

    public static final BigInteger COFACTOR =
        new BigInteger(
            "5d543a95414e7f1091d50792876a202cd91de4547085abaa68a205b2e5a7ddfa628f1cb4d9e82ef21537e293a6691ae1616ec6e786f0c70cf1c38e31c7238e5",
            16);
  }
}

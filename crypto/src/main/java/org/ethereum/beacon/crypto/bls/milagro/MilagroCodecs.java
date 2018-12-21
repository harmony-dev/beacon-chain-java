package org.ethereum.beacon.crypto.bls.milagro;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ECP;
import org.apache.milagro.amcl.BLS381.ECP2;
import org.apache.milagro.amcl.BLS381.FP;
import org.apache.milagro.amcl.BLS381.FP2;
import org.apache.milagro.amcl.BLS381.ROM;
import org.ethereum.beacon.crypto.bls.codec.Codec;
import org.ethereum.beacon.crypto.bls.codec.PointData;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public abstract class MilagroCodecs {
  private MilagroCodecs() {}

  private static final BIG Q = new BIG(ROM.Modulus);

  public static final Codec<ECP> G1 =
      new Codec<ECP>() {
        @Override
        public ECP decode(BytesValue encoded) {
          PointData.G1 g1 = Codec.G1.decode(encoded);
          if (g1.isInfinity()) {
            return new ECP();
          } else {
            BIG x = BIGs.fromByteArray(g1.getX());
            BIG y = ECP.RHS(new FP(x)).sqrt().redc();

            if (BIGs.getSign(y, Q) == g1.getSign()) {
              return new ECP(x, y);
            } else {
              return new ECP(x, BIGs.neg(y));
            }
          }
        }

        @Override
        public BytesValue encode(ECP point) {
          byte[] x = BIGs.toByteArray(point.getX());
          int sign = BIGs.getSign(point.getY(), Q);
          PointData.G1 data = PointData.G1.create(x, point.is_infinity(), sign);
          return data.encode();
        }
      };

  public static final Codec<ECP2> G2 =
      new Codec<ECP2>() {
        @Override
        public ECP2 decode(BytesValue encoded) {
          PointData.G2 g2 = Codec.G2.decode(encoded);
          if (g2.isInfinity()) {
            return new ECP2();
          } else {
            BIG im = BIGs.fromByteArray(g2.getX1());
            BIG re = BIGs.fromByteArray(g2.getX2());

            FP2 x = new FP2(re, im);
            FP2 y = ECP2.RHS(x);
            y.sqrt();

            if (BIGs.getSign(y.getB(), Q) == g2.getSign()) {
              return new ECP2(x, y);
            } else {
              y.neg();
              return new ECP2(x, y);
            }
          }
        }

        @Override
        public BytesValue encode(ECP2 point) {
          byte[] re = BIGs.toByteArray(point.getX().getA());
          byte[] im = BIGs.toByteArray(point.getX().getB());
          int sign = BIGs.getSign(point.getY().getB(), Q);
          PointData.G2 data = PointData.G2.create(im, re, point.is_infinity(), sign);
          return data.encode();
        }
      };
}

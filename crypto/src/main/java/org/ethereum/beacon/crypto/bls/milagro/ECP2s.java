package org.ethereum.beacon.crypto.bls.milagro;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ECP2;
import org.ethereum.beacon.crypto.bls.bc.BCParameters.G2;

public abstract class ECP2s {
  private ECP2s() {}

  private static final BIG COFACTOR_HIGH;
  private static final BIG COFACTOR_LOW;
  private static final BIG COFACTOR_SHIFT;

  static {
    byte[] bytes = G2.COFACTOR.toByteArray();
    assert bytes.length > BIG.MODBYTES;

    byte[] highPart = new byte[BIG.MODBYTES];
    byte[] lowPart = new byte[bytes.length - BIG.MODBYTES];

    System.arraycopy(bytes, 0, highPart, 0, BIG.MODBYTES);
    System.arraycopy(bytes, BIG.MODBYTES, lowPart, 0, bytes.length - BIG.MODBYTES);

    COFACTOR_HIGH = BIGs.fromByteArray(highPart, true);
    COFACTOR_LOW = BIGs.fromByteArray(lowPart, true);
    COFACTOR_SHIFT = new BIG(1);
    COFACTOR_SHIFT.shl(lowPart.length * 8);
  }

  public static ECP2 mulByCofactor(ECP2 point) {
    ECP2 product = point.mul(COFACTOR_HIGH).mul(COFACTOR_SHIFT);
    product.add(point.mul(COFACTOR_LOW));
    return product;
  }
}

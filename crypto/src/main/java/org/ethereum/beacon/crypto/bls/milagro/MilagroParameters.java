package org.ethereum.beacon.crypto.bls.milagro;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ROM;

/** Various {@code BLS12} elliptic curve parameters written with Milagro types and classes. */
public interface MilagroParameters {

  BIG ORDER = new BIG(ROM.CURVE_Order);

  abstract class Fp2 {
    private Fp2() {}

    public static final org.apache.milagro.amcl.BLS381.FP2 ONE =
        new org.apache.milagro.amcl.BLS381.FP2(1);
  }
}

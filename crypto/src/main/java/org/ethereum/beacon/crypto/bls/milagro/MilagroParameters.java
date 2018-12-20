package org.ethereum.beacon.crypto.bls.milagro;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ROM;

public interface MilagroParameters {

  abstract class G2 {
    private G2() {}

    public static final BIG COFACTOR = new BIG(ROM.CURVE_Cof);
  }

  abstract class FP2 {
    private FP2() {}

    public static final org.apache.milagro.amcl.BLS381.FP2 ONE =
        new org.apache.milagro.amcl.BLS381.FP2(1);
  }
}

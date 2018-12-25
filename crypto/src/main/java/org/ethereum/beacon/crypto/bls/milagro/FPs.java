package org.ethereum.beacon.crypto.bls.milagro;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.DBIG;
import org.apache.milagro.amcl.BLS381.FP;
import org.apache.milagro.amcl.BLS381.ROM;

/**
 * Various utility methods to work with Milagro implementation of a finite field <code>
 * F<sub>q</sub>.
 *
 * @see FP
 * @see BIG
 */
public class FPs {

  /** Field modulus. */
  private static final BIG Q = new BIG(ROM.Modulus);

  /**
   * Calculates a sign of the value in a finite field <code>F<sub>q</sub></code>.
   *
   * <p>A sign is calculated by next formula: {@code sign = (value * 2) / q}.
   *
   * @param value a value.
   * @return calculated sign.
   */
  public static int getSign(BIG value) {
    DBIG d = new DBIG(value);
    d.add(d);
    return d.div(Q).bit(0);
  }

  /**
   * Negates value in a field <code>F<sub>q</sub></code>.
   *
   * @param value a value.
   * @return negated value.
   */
  public static BIG neg(BIG value) {
    FP fp = new FP(value);
    fp.neg();
    return fp.redc();
  }
}

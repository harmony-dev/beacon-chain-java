package org.ethereum.beacon.crypto.bls.milagro;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ECP2;
import org.apache.milagro.amcl.BLS381.FP2;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.MessageParametersMapper;
import org.ethereum.beacon.crypto.bls.milagro.MilagroParameters.Fp2;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.bytes.BytesValues;

public class MilagroMessageMapper implements MessageParametersMapper<ECP2> {

  private static final BytesValue BYTES_ONE = BytesValues.ofUnsignedByte(1);
  private static final BytesValue BYTES_TWO = BytesValues.ofUnsignedByte(2);

  @Override
  public ECP2 map(MessageParameters parameters) {
    BytesValue reBytes = parameters.getDomain().concat(BYTES_ONE).concat(parameters.getHash());
    BytesValue imBytes = parameters.getDomain().concat(BYTES_TWO).concat(parameters.getHash());

    BIG reX = BIGs.fromBytes(Hashes.keccack384(reBytes));
    BIG imX = BIGs.fromBytes(Hashes.keccack384(imBytes));

    FP2 x = new FP2(reX, imX);
    org.apache.milagro.amcl.BLS381.ECP2 point = new org.apache.milagro.amcl.BLS381.ECP2(x);
    while (point.is_infinity()) {
      x.add(Fp2.ONE);
      point = new org.apache.milagro.amcl.BLS381.ECP2(x);
    }

    return ECP2s.mulByCofactor(point);
  }
}

package org.ethereum.beacon.crypto.bls.milagro;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ECP2;
import org.apache.milagro.amcl.BLS381.FP2;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.MessageParametersMapper;
import org.ethereum.beacon.crypto.bls.milagro.MilagroParameters.Fp2;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;

/**
 * Message mapper that works with Milagro implementation of points of elliptic curve defined over
 * <code>F<sub>q<sup>2</sup></sub></code>.
 *
 * <p>Mapping algorithm is described in the spec <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md#hash_to_g2">https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md#hash_to_g2</a>
 *
 * @see MessageParametersMapper
 * @see MessageParameters
 * @see ECP2
 */
public class MilagroMessageMapper implements MessageParametersMapper<ECP2> {

  private static final BytesValue BYTES_ONE = BytesValues.ofUnsignedByte(1);
  private static final BytesValue BYTES_TWO = BytesValues.ofUnsignedByte(2);

  @Override
  public ECP2 map(MessageParameters parameters) {
    BytesValue reBytes = parameters.getHash().concat(parameters.getDomain()).concat(BYTES_ONE);
    BytesValue imBytes = parameters.getHash().concat(parameters.getDomain()).concat(BYTES_TWO);

    BIG reX = BIGs.fromBytes(Hashes.keccak256(reBytes));
    BIG imX = BIGs.fromBytes(Hashes.keccak256(imBytes));

    FP2 x = new FP2(reX, imX);
    org.apache.milagro.amcl.BLS381.ECP2 point = new org.apache.milagro.amcl.BLS381.ECP2(x);
    while (point.is_infinity()) {
      x.add(Fp2.ONE);
      point = new org.apache.milagro.amcl.BLS381.ECP2(x);
    }

    return ECP2s.mulByG2Cofactor(point);
  }
}

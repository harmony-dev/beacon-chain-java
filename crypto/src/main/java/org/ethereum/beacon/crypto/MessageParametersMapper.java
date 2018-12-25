package org.ethereum.beacon.crypto;

import org.ethereum.beacon.crypto.bls.milagro.MilagroMessageMapper;

/**
 * An interface of a mapper that coverts message to {@code BLS12} elliptic curve point.
 *
 * <p>One of its possible implementations described in the spec <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md#hash_to_g2"></a>
 *
 * @param <P> point type.
 * @see MessageParameters
 * @see MilagroMessageMapper
 */
public interface MessageParametersMapper<P> {

  /**
   * Calculates a message representation on elliptic curve.
   *
   * @param parameters parameters of the message.
   * @return point on elliptic curve.
   */
  P map(MessageParameters parameters);
}

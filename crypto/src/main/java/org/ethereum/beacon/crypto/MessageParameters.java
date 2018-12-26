package org.ethereum.beacon.crypto;

import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * An interface of message parameters that are used by BLS381 signature scheme.
 *
 * <p>According to the spec, message parameters are its hash and domain.
 *
 * @see MessageParametersMapper
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md">https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md</a>
 */
public interface MessageParameters {

  /**
   * Returns a hash of the message.
   *
   * @return hash value.
   */
  Bytes32 getHash();

  /**
   * Returns message domain.
   *
   * @return domain value.
   */
  BytesValue getDomain();

  /** A straightforward implementation of {@link MessageParameters}. */
  class Impl implements MessageParameters {
    private final Bytes32 hash;
    private final BytesValue domain;

    public Impl(Bytes32 hash, BytesValue domain) {
      this.hash = hash;
      this.domain = domain;
    }

    @Override
    public Bytes32 getHash() {
      return hash;
    }

    @Override
    public BytesValue getDomain() {
      return domain;
    }
  }
}

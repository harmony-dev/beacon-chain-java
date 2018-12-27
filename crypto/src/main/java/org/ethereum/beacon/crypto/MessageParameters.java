package org.ethereum.beacon.crypto;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
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
  Hash32 getHash();

  /**
   * Returns message domain.
   *
   * @return domain value.
   */
  Bytes8 getDomain();

  /** A straightforward implementation of {@link MessageParameters}. */
  class Impl implements MessageParameters {
    private final Hash32 hash;
    private final Bytes8 domain;

    public Impl(Hash32 hash, Bytes8 domain) {
      this.hash = hash;
      this.domain = domain;
    }

    @Override
    public Hash32 getHash() {
      return hash;
    }

    @Override
    public Bytes8 getDomain() {
      return domain;
    }
  }
}

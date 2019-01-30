package org.ethereum.beacon.validator.crypto;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;

/**
 * An interface of helpers producing message signatures.
 *
 * @param <S> a type of signature instances.
 */
public interface MessageSigner<S> {

  /**
   * Signs of on a given message parameters.
   *
   * @param messageHash a hash of a message.
   * @param domain a message domain.
   * @return signature.
   */
  S sign(Hash32 messageHash, Bytes8 domain);
}

package org.ethereum.beacon.validator.crypto;

import tech.pegasys.artemis.util.bytes.Bytes96;

/**
 * BLS381 message signer.
 *
 * @see MessageSigner
 */
public interface BLS381MessageSigner extends MessageSigner<Bytes96> {}

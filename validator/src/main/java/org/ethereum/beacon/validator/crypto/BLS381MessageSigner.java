package org.ethereum.beacon.validator.crypto;

import org.ethereum.beacon.core.types.BLSSignature;

/**
 * BLS381 message signer.
 *
 * @see MessageSigner
 */
public interface BLS381MessageSigner extends MessageSigner<BLSSignature> {}

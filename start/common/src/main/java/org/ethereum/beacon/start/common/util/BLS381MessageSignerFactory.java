package org.ethereum.beacon.start.common.util;

import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.validator.crypto.BLS381MessageSigner;

public interface BLS381MessageSignerFactory {
  static BLS381MessageSignerFactory getInsecureFactory() {
    return keyPair -> (messageHash, domain) -> BLSSignature.wrap(BLS381.sign(MessageParameters.create(messageHash, domain), keyPair).getEncoded());
  }

  BLS381MessageSigner createSigner(BLS381.KeyPair keyPair);
}

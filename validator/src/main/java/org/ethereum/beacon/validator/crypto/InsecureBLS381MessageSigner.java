package org.ethereum.beacon.validator.crypto;

import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.crypto.MessageParameters;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;

/**
 * Insecure BLS381 message signer.
 *
 * <p>Instantiated with bytes of private key which are kept in memory all the way. Not a production
 * solution.
 */
public class InsecureBLS381MessageSigner implements BLS381MessageSigner {

  private final Bytes32 privateKeyBytes;

  public InsecureBLS381MessageSigner(Bytes32 privateKeyBytes) {
    this.privateKeyBytes = privateKeyBytes;
  }

  @Override
  public Bytes96 sign(Hash32 messageHash, Bytes8 domain) {
    MessageParameters messageParameters = MessageParameters.create(messageHash, domain);
    KeyPair keyPair = KeyPair.create(PrivateKey.create(privateKeyBytes));
    return BLS381.sign(messageParameters, keyPair).getEncoded();
  }
}

package org.ethereum.beacon.validator;

import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.validator.crypto.InsecureBLS381MessageSigner;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.util.bytes.Bytes32;

public class MessageSignerTestUtil {

  public static MessageSigner<BLSSignature> createBLSSigner() {
    Bytes32 privateKey = BLS381.KeyPair.generate().getPrivate().getEncodedBytes();
    return new InsecureBLS381MessageSigner(privateKey);
  }
}

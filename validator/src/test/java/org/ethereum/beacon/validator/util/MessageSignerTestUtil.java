package org.ethereum.beacon.validator.util;

import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.validator.crypto.InsecureBLS381MessageSigner;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes96;

public class MessageSignerTestUtil {

  public static MessageSigner<Bytes96> createBLSSigner() {
    Bytes32 privateKey = BLS381.KeyPair.generate().getPrivate().getEncodedBytes();
    return new InsecureBLS381MessageSigner(privateKey);
  }
}

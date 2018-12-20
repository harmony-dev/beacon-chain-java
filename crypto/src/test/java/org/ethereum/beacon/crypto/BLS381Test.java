package org.ethereum.beacon.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.bls.codec.Codec;
import org.junit.Test;

public class BLS381Test {

  @Test
  public void checkKeyPairGeneration() {
    KeyPair keyPair = BLS381.KeyPair.generate();
    assertThat(keyPair.getPrivate().getEncoded().length).isEqualTo(32);
    assertThat(keyPair.getPublic().getEncoded().length).isEqualTo(Codec.G1_ENCODED_BYTES);
  }


}

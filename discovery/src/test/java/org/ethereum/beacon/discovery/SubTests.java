package org.ethereum.beacon.discovery;

import org.ethereum.beacon.util.Utils;
import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

/**
 * Secondary tests not directly related to discovery but clarifying functions used somewhere in
 * discovery routines
 */
public class SubTests {
  /**
   * Tests BigInteger to byte[]. Take a look at {@link
   * Utils#extractBytesFromUnsignedBigInt(BigInteger)} for understanding the issue.
   */
  @Test
  public void testPubKeyBadPrefix() {
    BytesValue privKey =
        BytesValue.fromHexString(
            "0xade78b68f25611ea57532f86bf01da909cc463465ed9efce9395403ff7fc99b5");
    ECKeyPair badKey = ECKeyPair.create(privKey.extractArray());
    byte[] pubKey = Utils.extractBytesFromUnsignedBigInt(badKey.getPublicKey());
    assertEquals(64, pubKey.length);
  }
}

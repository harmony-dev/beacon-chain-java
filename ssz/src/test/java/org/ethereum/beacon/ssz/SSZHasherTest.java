package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.fixtures.AttestationRecord;
import org.ethereum.beacon.ssz.fixtures.Bitfield;
import org.ethereum.beacon.ssz.fixtures.Sign;
import org.junit.Before;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;

import static com.sun.org.apache.xerces.internal.impl.dv.util.HexBin.decode;
import static org.junit.Assert.assertEquals;

/**
 * Tests of {@link SSZHasher}
 */
public class SSZHasherTest {
  private SSZHasher sszHasher;

  @Before
  public void setup() {
    sszHasher = new SSZHasher((data) -> Bytes.wrap(Hashes.keccak256(BytesValue.of(data.toArrayUnsafe())).getArrayUnsafe()));
  }

  private static byte[] DEFAULT_HASH = Hashes.keccak256(BytesValue.fromHexString("aa")).getArrayUnsafe();
  private static Sign.Signature DEFAULT_SIG = new Sign.Signature();
  static {
    SecureRandom random = new SecureRandom();
    byte[] r = new byte[20];
    random.nextBytes(r);
    DEFAULT_SIG.r = new BigInteger(1, r);
    byte[] s = new byte[20];
    random.nextBytes(s);
    DEFAULT_SIG.s = new BigInteger(1, s);
  }

  @Test
  public void bitfieldTest() {
    Bitfield bitfield = new Bitfield(
        decode("abcd")
    );

    Bytes hash = sszHasher.calc(bitfield);

    assertEquals(Bytes.fromHexString("aa"), hash);
  }

  @Test
  public void SignatureTest() {
    Sign.Signature signature = new Sign.Signature();
    signature.r = new BigInteger("23452342342342342342342315643768758756967967");
    signature.s = new BigInteger("8713785871");

    Bytes hash = sszHasher.calc(signature);

    assertEquals(Bytes.fromHexString("aa"), hash);
  }

  @Test
  public void simpleTest() {
    AttestationRecord attestationRecord = new AttestationRecord(
        12412L,
        123,
        Collections.emptyList(),
        DEFAULT_HASH,
        new Bitfield(decode("abcdef45")),
        12400L,
        DEFAULT_HASH,
        DEFAULT_SIG
    );

    Bytes hash = sszHasher.calc(attestationRecord);

    assertEquals(Bytes.fromHexString("aa"), hash);
  }
}

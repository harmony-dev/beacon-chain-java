package net.consensys.cava.ssz;

import net.consensys.cava.ssz.fixtures.AttestationRecord;
import net.consensys.cava.ssz.fixtures.Bitfield;
import net.consensys.cava.ssz.fixtures.Sign;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.util.ssz.SSZAnnotationSchemeBuilder;
import org.ethereum.beacon.util.ssz.SSZSerializer;
import org.junit.Before;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;

import static com.sun.org.apache.xerces.internal.impl.dv.util.HexBin.decode;
import static org.junit.Assert.assertEquals;

public class SSZSerializerTest {
  private SSZSerializer sszSerializer;

  @Before
  public void setup() {
    sszSerializer = new SSZSerializer(new SSZAnnotationSchemeBuilder());
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
    Bitfield expected = new Bitfield(
        decode("abcd")
    );

    byte[] encoded = sszSerializer.encode(expected);
    Bitfield constructed = (Bitfield) sszSerializer.decode(encoded, Bitfield.class);

    assertEquals(expected, constructed);
  }

  @Test
  public void SignatureTest() {
    Sign.Signature signature = new Sign.Signature();
    signature.r = new BigInteger("23452342342342342342342315643768758756967967");
    signature.s = new BigInteger("8713785871");

    byte[] encoded = sszSerializer.encode(signature);
    Sign.Signature constructed = (Sign.Signature) sszSerializer.decode(encoded, Sign.Signature.class);

    assertEquals(signature, constructed);
  }

  @Test
  public void simpleTest() {
    AttestationRecord expected = new AttestationRecord(
        12412L,
        123,
        Collections.emptyList(),
        DEFAULT_HASH,
        new Bitfield(decode("abcdef45")),
        12400L,
        DEFAULT_HASH,
        DEFAULT_SIG
    );

    byte[] encoded = sszSerializer.encode(expected);
    AttestationRecord constructed = (AttestationRecord) sszSerializer.decode(encoded, AttestationRecord.class);

    assertEquals(expected, constructed);
  }

  @Test
  public void nullableTest() {
    AttestationRecord expected1 = new AttestationRecord(
        12412L,
        123,
        Collections.emptyList(),
        DEFAULT_HASH,
        new Bitfield(decode("abcdef45")),
        12400L,
        DEFAULT_HASH,
        null
    );
    byte[] encoded1 = sszSerializer.encode(expected1);
    AttestationRecord actual1 = (AttestationRecord) sszSerializer.decode(encoded1, AttestationRecord.class);

    assertEquals(expected1, actual1);

    AttestationRecord expected2 = new AttestationRecord(
        12412L,
        123,
        Collections.emptyList(),
        DEFAULT_HASH,
        null,
        12400L,
        DEFAULT_HASH,
        DEFAULT_SIG
    );
    byte[] encoded2 = sszSerializer.encode(expected2);
    AttestationRecord actual2 = (AttestationRecord) sszSerializer.decode(encoded2, AttestationRecord.class);

    assertEquals(expected2, actual2);

    AttestationRecord expected3 = new AttestationRecord(
        12412L,
        123,
        Collections.emptyList(),
        DEFAULT_HASH,
        null,
        12400L,
        DEFAULT_HASH,
        null
    );
    byte[] encoded3 = sszSerializer.encode(expected3);
    AttestationRecord actual3 = (AttestationRecord) sszSerializer.decode(encoded3, AttestationRecord.class);

    assertEquals(expected3, actual3);
  }

  @Test(expected = NullPointerException.class)
  public void nullFixedSizeFieldTest() {
    AttestationRecord expected3 = new AttestationRecord(
        12412L,
        123,
        Collections.emptyList(),
        null,
        new Bitfield(decode("abcdef45")),
        12400L,
        null,
        DEFAULT_SIG
    );
    sszSerializer.encode(expected3);
  }

  @Test(expected = NullPointerException.class)
  public void nullListTest() {
    AttestationRecord expected4 = new AttestationRecord(
        12412L,
        123,
        null,
        DEFAULT_HASH,
        new Bitfield(decode("abcdef45")),
        12400L,
        DEFAULT_HASH,
        DEFAULT_SIG
    );
    sszSerializer.encode(expected4);
  }
}

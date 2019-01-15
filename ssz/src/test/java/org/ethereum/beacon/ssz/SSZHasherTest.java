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
    DEFAULT_SIG.r = new BigInteger("23452342342342342342342315643768758756967967");
    DEFAULT_SIG.s = new BigInteger("8713785871");
  }

  @Test
  public void bitfieldTest() {
    Bitfield bitfield = new Bitfield(
        decode("abcd")
    );

    Bytes hash = sszHasher.calc(bitfield);

    assertEquals(Bytes.fromHexString("DB8971730132CFCB1BFA9A3E5E3DFD79442BDC1A1E8672633B838E29D392C7AD"), hash);
  }

  @Test
  public void SignatureTest() {
    Bytes hash = sszHasher.calc(DEFAULT_SIG);

    assertEquals(Bytes.fromHexString("D75724A07F4EFB3B456408DD6C36C70A6DF189FAE6A09F7AD0C848F0D3341290"), hash);
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

    assertEquals(Bytes.fromHexString("CC4B9A044445D67276E0FE4B11833912DE98A3BE2382A400383C484C1E424FF7"), hash);
  }
}

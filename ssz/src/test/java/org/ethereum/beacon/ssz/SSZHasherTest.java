package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.fixtures.AttestationRecord;
import org.ethereum.beacon.ssz.fixtures.Bitfield;
import org.ethereum.beacon.ssz.fixtures.Sign;
import org.junit.Before;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  @Test
  public void list32Test() {
    List<byte[]> hashes = new ArrayList<>();
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("aa")).getArrayUnsafe());
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("bb")).getArrayUnsafe());
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("cc")).getArrayUnsafe());
    AttestationRecord attestationRecord = new AttestationRecord(
        12412L,
        123,
        hashes,
        DEFAULT_HASH,
        new Bitfield(decode("abcdef45")),
        12400L,
        DEFAULT_HASH,
        DEFAULT_SIG
    );

    Bytes hash = sszHasher.calc(attestationRecord);
    assertEquals(Bytes.fromHexString("EF30C4DEF04E0DC4D71188C7C99C7A89D953BA2ABC6E9E7CFC438E3EFA62F4F9"), hash);
  }

  @Test
  public void list48Test() {
    List<byte[]> hashes = new ArrayList<>();
    hashes.add(Hashes.keccak384(BytesValue.fromHexString("aa")).getArrayUnsafe());
    hashes.add(Hashes.keccak384(BytesValue.fromHexString("bb")).getArrayUnsafe());
    hashes.add(Hashes.keccak384(BytesValue.fromHexString("cc")).getArrayUnsafe());
    AttestationRecord attestationRecord = new AttestationRecord(
        12412L,
        123,
        hashes,
        DEFAULT_HASH,
        new Bitfield(decode("abcdef45")),
        12400L,
        DEFAULT_HASH,
        DEFAULT_SIG
    );

    Bytes hash = sszHasher.calc(attestationRecord);
    assertEquals(Bytes.fromHexString("AC01DF693CF95F1486AF5CEFCB4DD3D55312AF70A52A56E5B2394C00A887DAC7"), hash);
  }

  @SSZSerializable
  public static class SomeObject {
    private List<Long> list;

    public SomeObject(List<Long> list) {
      this.list = list;
    }

    public List<Long> getList() {
      return list;
    }
  }

  @Test
  public void smallItemsListTest() {
    List<Long> list = new ArrayList<>();
    list.add(1L);
    list.add(2L);
    list.add(12345L);
    list.add(Long.MAX_VALUE);
    SomeObject someObject = new SomeObject(list);

    Bytes hash = sszHasher.calc(someObject);
    assertEquals(Bytes.fromHexString("BD4AB28F883B78BF4C5B3652AFCF272EAD9026C3361821A0420777A9B3C20425"), hash);
  }

  @SSZSerializable
  public static class AnotherObject {
    private int item;

    public AnotherObject(int item) {
      this.item = item;
    }

    public int getItem() {
      return item;
    }
  }

  @Test
  public void smallItemTest() {
    AnotherObject anotherObject1 = new AnotherObject(1);
    AnotherObject anotherObject2 = new AnotherObject(2);

    Bytes hash1 = sszHasher.calc(anotherObject1);
    Bytes hash2 = sszHasher.calc(anotherObject2);
    assertEquals(Bytes.fromHexString("95DBAD4637B631C083A4BBEEF4E3D609C32941D9997A3AEC4A123AAA0671F41B"), hash1);
    assertEquals(Bytes.fromHexString("DBC5B7FCA0CFD77A68E44FE301A3B706EC4F12ECD153785297D28A936E64404A"), hash2);
  }
}

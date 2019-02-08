package org.ethereum.beacon.consensus.hasher;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.SSZHashSerializers;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.fixtures.AttestationRecord;
import org.ethereum.beacon.ssz.fixtures.Bitfield;
import org.ethereum.beacon.ssz.fixtures.Sign;
import org.junit.Before;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Tests of {@link SSZObjectHasher} */
public class SSZObjectHasherTest {
  private static byte[] DEFAULT_HASH =
      Hashes.keccak256(BytesValue.fromHexString("aa")).getArrayUnsafe();
  private static Sign.Signature DEFAULT_SIG = new Sign.Signature();

  static {
    DEFAULT_SIG.r = new BigInteger("23452342342342342342342315643768758756967967");
    DEFAULT_SIG.s = new BigInteger("8713785871");
  }

  private SSZObjectHasher sszHasher;

  @Before
  public void setup() {
    SSZSerializer sszHashSerializer =
        SSZHashSerializers.createWithBeaconChainTypes(Hashes::keccak256, false);
    sszHasher = new SSZObjectHasher<>(sszHashSerializer, Hashes::keccak256);
  }

  @Test
  public void bitfieldTest() {
    Bitfield bitfield = new Bitfield(BytesValue.fromHexString("abcd").getArrayUnsafe());

    BytesValue hash = sszHasher.getHash(bitfield);
    assertEquals(
        BytesValue.fromHexString(
            "A0B1BE2F50398CA7FE11EA48E5AFE9F89F758EC815E5C12BE21315AF6D34FA1D"),
        hash);
  }

  @Test
  public void SignatureTest() {
    BytesValue hash = sszHasher.getHash(DEFAULT_SIG);
    assertEquals(
        BytesValue.fromHexString(
            "D75724A07F4EFB3B456408DD6C36C70A6DF189FAE6A09F7AD0C848F0D3341290"),
        hash);
  }

  @Test
  public void simpleTest() {
    AttestationRecord attestationRecord =
        new AttestationRecord(
            123,
            Collections.emptyList(),
            DEFAULT_HASH,
            new Bitfield(BytesValue.fromHexString("abcdef45").getArrayUnsafe()),
            DEFAULT_HASH,
            12412L,
            12400L,
            DEFAULT_SIG);

    BytesValue hash = sszHasher.getHash(attestationRecord);
    assertEquals(
        BytesValue.fromHexString(
            "740620beb3f42033473a7adf01b5f115ec0a72bf8c97eb36f732a6158ff8775d"),
        hash);
  }

  @Test
  public void list32Test() {
    List<byte[]> hashes = new ArrayList<>();
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("aa")).getArrayUnsafe());
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("bb")).getArrayUnsafe());
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("cc")).getArrayUnsafe());
    AttestationRecord attestationRecord =
        new AttestationRecord(
            123,
            Collections.emptyList(),
            DEFAULT_HASH,
            new Bitfield(BytesValue.fromHexString("abcdef45").getArrayUnsafe()),
            DEFAULT_HASH,
            12412L,
            12400L,
            DEFAULT_SIG);

    BytesValue hash = sszHasher.getHash(attestationRecord);
    assertEquals(
        BytesValue.fromHexString(
            "740620beb3f42033473a7adf01b5f115ec0a72bf8c97eb36f732a6158ff8775d"),
        hash);
  }

  @Test
  public void smallItemsListTest() {
    List<Long> list = new ArrayList<>();
    list.add(1L);
    list.add(2L);
    list.add(12345L);
    list.add(Long.MAX_VALUE);
    SomeObject someObject = new SomeObject(list);

    BytesValue hash = sszHasher.getHash(someObject);
    assertEquals(
        BytesValue.fromHexString(
            "BD4AB28F883B78BF4C5B3652AFCF272EAD9026C3361821A0420777A9B3C20425"),
        hash);
  }

  @Test
  public void smallItemTest() {
    AnotherObject anotherObject1 = new AnotherObject(1);
    AnotherObject anotherObject2 = new AnotherObject(2);

    BytesValue hash1 = sszHasher.getHash(anotherObject1);
    BytesValue hash2 = sszHasher.getHash(anotherObject2);
    assertEquals(
        BytesValue.fromHexString(
            "FB5BAAECAB62C516763CEA2DFBA17FBBC24907E4E3B0BE426BDE71BE89AF495F"),
        hash1);
    assertEquals(
        BytesValue.fromHexString(
            "B7047395B0D5A9C70336FDE7E40DE2BB369FE67C8E762A35641E209B7338FDD9"),
        hash2);
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
}

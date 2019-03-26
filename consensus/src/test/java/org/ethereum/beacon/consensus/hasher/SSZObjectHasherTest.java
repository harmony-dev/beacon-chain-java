package org.ethereum.beacon.consensus.hasher;

import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.SSZHashSerializer;
import org.ethereum.beacon.ssz.SSZHashSerializers;
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

import static org.junit.Assert.assertEquals;

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
    SSZHashSerializer sszHashSerializer =
        SSZHashSerializers.createWithBeaconChainTypes(Hashes::keccak256, false, 128);
    sszHasher = new SSZObjectHasher(sszHashSerializer);
  }

  @Test
  public void bitfieldTest() {
    Bitfield bitfield = new Bitfield(BytesValue.fromHexString("abcd").getArrayUnsafe());

    BytesValue hash = sszHasher.getHash(bitfield);
    assertEquals(
        BytesValue.fromHexString(
            "0x02000000abcd0000000000000000000000000000000000000000000000000000"),
        hash);
  }

  @Test
  public void SignatureTest() {
    BytesValue hash = sszHasher.getHash(DEFAULT_SIG);
    assertEquals(
        BytesValue.fromHexString(
            "0x3d15cc04a0a366f8e0bc034db6df008f9eaf30d7bd0b1b40c4bd7bd141bd42f7"),
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
            "0x3dfd0d63b835618cc9eb5f5da13b494b0e4ab41583b66809fed6fc4990f4dd51"),
        hash);
  }

  @Test
  public void simpleTruncateTest() {
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

    // justifiedBlockHash and aggregateSig removed
    BytesValue hash1 = sszHasher.getHashTruncate(attestationRecord, "justifiedBlockHash");
    assertEquals(
        BytesValue.fromHexString(
            "0x945b6a8eac7bd3611f6fb452fd7f63d77ce3672752df45443beb0e0169bf33cb"),
        hash1);

    // Sig only removed
    BytesValue hash2 = sszHasher.getHashTruncate(attestationRecord, "aggregateSig");
    assertEquals(
        BytesValue.fromHexString(
            "0xae3f28da5903192bff0472fc12baf3acb8c2554606c2449f833d2079188eb871"),
        hash2);

    boolean fired = false;
    try {
      // No such field
      BytesValue hash3 = sszHasher.getHashTruncate(attestationRecord, "myField");
      assertEquals(
          BytesValue.fromHexString(
              "740620beb3f42033473a7adf01b5f115ec0a72bf8c97eb36f732a6158ff8775d"),
          hash3);
    } catch (Exception e) {
      if (e.getMessage().contains("myField")) {
        fired = true;
      }
    }

    assert fired;
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
            "0x3dfd0d63b835618cc9eb5f5da13b494b0e4ab41583b66809fed6fc4990f4dd51"),
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
            "0xb1a18810e9b465f89b07c45716aef51cb243892a9ca24b37a4c322752fb905d6"),
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
            "0x0100000000000000000000000000000000000000000000000000000000000000"),
        hash1);
    assertEquals(
        BytesValue.fromHexString(
            "0x0200000000000000000000000000000000000000000000000000000000000000"),
        hash2);
  }

  @Test
  public void listTest() {
    AnotherObject anotherObject1 = new AnotherObject(1);
    AnotherObject anotherObject2 = new AnotherObject(2);
    List<AnotherObject> anotherObjects = new ArrayList<>();
    anotherObjects.add(anotherObject1);
    anotherObjects.add(anotherObject2);
    BytesValue hash = sszHasher.getHash(anotherObjects);
    assertEquals(
        BytesValue.fromHexString(
            "0x6d3a1eb14c6b37eb4645044d0c1bf38284b408eab24e89238a8058f3b921e5d9"),
        hash);
  }

  @Test
  public void listTest2() {
    List<ValidatorIndex> list = new ArrayList<>();
    list.add(ValidatorIndex.of(1));
    list.add(ValidatorIndex.of(1));
    BytesValue hash = sszHasher.getHash(list);
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

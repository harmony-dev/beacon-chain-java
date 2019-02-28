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
        SSZHashSerializers.createWithBeaconChainTypes(Hashes::keccak256, false);
    sszHasher = new SSZObjectHasher(sszHashSerializer);
  }

  @Test
  public void bitfieldTest() {
    Bitfield bitfield = new Bitfield(BytesValue.fromHexString("abcd").getArrayUnsafe());

    BytesValue hash = sszHasher.getHash(bitfield);
    assertEquals(
        BytesValue.fromHexString(
            "0x71d6fb668707a083955a758669047e129cd7f73da7f854b60f8a809dad13e640"),
        hash);
  }

  @Test
  public void SignatureTest() {
    BytesValue hash = sszHasher.getHash(DEFAULT_SIG);
    assertEquals(
        BytesValue.fromHexString(
            "0xdf395bd0ff83202099e844b8fd46da57d7c9b0f0af7fae875ad562877b318245"),
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
            "0xf9c649a6265388db3a012efadd34c5f426371e90450bbf83ee4020476912b3bf"),
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
            "0x1f1c7368d948a8e741f8ce82a2493dbf192080f38926829e47be6d1d042117eb"),
        hash1);

    // Sig only removed
    BytesValue hash2 = sszHasher.getHashTruncate(attestationRecord, "aggregateSig");
    assertEquals(
        BytesValue.fromHexString(
            "0x0a6a337dda9092627f55f964c925da72dbc74b028f0c7b1aa6ea7305fc82b050"),
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
            "0xf9c649a6265388db3a012efadd34c5f426371e90450bbf83ee4020476912b3bf"),
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
            "0xe28e68f5df2f8a5b8740d2f8d93fcc6733577e629963962db2c39f9794368b30"),
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
            "0x8e4ddd4f8d4d3c485c82deeff4f187d60a0f3bce8a3ac0efd2a93c914a4edbd9"),
        hash1);
    assertEquals(
        BytesValue.fromHexString(
            "0xa42fa748506bbdec60a9ccc23a74d51743e91f7d4587816bd1ffbb501bb3779c"),
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
            "0x5700f35a92bf27fba1543e39a11d9a077321d7b67710a7cc8bafaf81474768e3"),
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

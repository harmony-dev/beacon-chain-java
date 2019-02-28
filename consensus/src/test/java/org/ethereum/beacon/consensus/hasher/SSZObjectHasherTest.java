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
    sszHasher = new SSZObjectHasher(sszHashSerializer, Hashes::keccak256);
  }

  @Test
  public void bitfieldTest() {
    Bitfield bitfield = new Bitfield(BytesValue.fromHexString("abcd").getArrayUnsafe());

    BytesValue hash = sszHasher.getHash(bitfield);
    assertEquals(
        BytesValue.fromHexString(
            "0x1487e072053dbcdc89f662bd1ddf36a2f22c3d0594c511d349dfa18e595d6d59"),
        hash);
  }

  @Test
  public void SignatureTest() {
    BytesValue hash = sszHasher.getHash(DEFAULT_SIG);
    assertEquals(
        BytesValue.fromHexString(
            "0x479653058b37be6734f8f2b5ab589be2065a4decc77cfc2ad03f883ce0c9e609"),
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
            "0x93a79f0374c8cac12d3234f7aaeee20cd5a9902bfba7dd85a21d1cceb836921e"),
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
            "0xac0c6c3e2f2d6f5af8ee3d41729018acf3aa5d8f36ea1241706c213b4cb88fc0"),
        hash1);

    // Sig only removed
    BytesValue hash2 = sszHasher.getHashTruncate(attestationRecord, "aggregateSig");
    assertEquals(
        BytesValue.fromHexString(
            "0x1d54a52a350790809fb6fd16968671390f074504fbf771e4dae8c5753e07e43d"),
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
            "0x93a79f0374c8cac12d3234f7aaeee20cd5a9902bfba7dd85a21d1cceb836921e"),
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
            "0xda54f4e7a1c2a874d9589bd283278d61433f4c21de313291efba440874d07bc3"),
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
            "0x48078cfed56339ea54962e72c37c7f588fc4f8e5bc173827ba75cb10a63a96a5"),
        hash1);
    assertEquals(
        BytesValue.fromHexString(
            "0x340dd630ad21bf010b4e676dbfa9ba9a02175262d1fa356232cfde6cb5b47ef2"),
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
            "0xac239c6a7397cc176933c6b1a4d75df567c2603e49f53ea61878b1e484b62fbe"),
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

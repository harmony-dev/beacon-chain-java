package org.ethereum.beacon.ssz;

import java.util.Arrays;
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.SSZ;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.access.container.SSZAnnotationSchemeBuilder;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.creator.ConstructorObjCreator;
import org.ethereum.beacon.ssz.creator.CompositeObjCreator;
import org.ethereum.beacon.ssz.fixtures.AttestationRecord;
import org.ethereum.beacon.ssz.fixtures.Bitfield;
import org.ethereum.beacon.ssz.fixtures.Sign;
import org.ethereum.beacon.ssz.access.basic.UIntCodec;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import tech.pegasys.artemis.util.uint.UInt64;

import static net.consensys.cava.bytes.Bytes.fromHexString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** Tests of {@link SSZSerializer} */
public class SSZSerializerTest {
  private static byte[] DEFAULT_HASH =
      Hashes.keccak256(BytesValue.fromHexString("aa")).getArrayUnsafe();
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

  private SSZSerializer sszSerializer;

  @Before
  public void setup() {
    sszSerializer = new SSZBuilder().withExplicitAnnotations(false).buildSerializer();
  }

  @Test
  public void bitfieldTest() {
    Bitfield expected = new Bitfield(BytesValue.fromHexString("abcd").getArrayUnsafe());

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
    Sign.Signature constructed =
        sszSerializer.decode(encoded, Sign.Signature.class);

    assertEquals(signature, constructed);
  }

  @Test
  public void simpleTest() {
    AttestationRecord expected =
        new AttestationRecord(
            123,
            Collections.emptyList(),
            DEFAULT_HASH,
            new Bitfield(BytesValue.fromHexString("abcdef45").getArrayUnsafe()),
            DEFAULT_HASH,
            12412L,
            12400L,
            DEFAULT_SIG);

    byte[] encoded = sszSerializer.encode(expected);
    AttestationRecord constructed =
        sszSerializer.decode(encoded, AttestationRecord.class);

    assertEquals(expected, constructed);
  }

  @Test
  public void explicitAnnotationsAndLoggerTest() {
    SSZBuilder builder = new SSZBuilder(); //
    builder
        .withSSZSchemeBuilder(new SSZAnnotationSchemeBuilder().withLogger(Logger.getLogger("test")))
//        .withSSZCodecResolver(new SSZCodecRoulette())
        .withObjectCreator(new CompositeObjCreator(new ConstructorObjCreator()));
    builder.addDefaultBasicCodecs();
    SSZSerializer serializer = builder.buildSerializer();

    AttestationRecord expected =
        new AttestationRecord(
            123,
            Collections.emptyList(),
            DEFAULT_HASH,
            new Bitfield(BytesValue.fromHexString("abcdef45").getArrayUnsafe()),
            DEFAULT_HASH,
            12412L,
            12400L,
            DEFAULT_SIG);

    byte[] encoded = serializer.encode(expected);
    AttestationRecord constructed =
        serializer.decode(encoded, AttestationRecord.class);

    Assert.assertNotEquals(expected, constructed);

    assertEquals(expected.getShardId(), constructed.getShardId());
    assertEquals(expected.getObliqueParentHashes(), constructed.getObliqueParentHashes());
    Assert.assertArrayEquals(expected.getShardBlockHash(), constructed.getShardBlockHash());
    Assert.assertNull(constructed.getAggregateSig());
  }

  @Test(expected = NullPointerException.class)
  public void nullFixedSizeFieldTest() {
    AttestationRecord expected3 =
        new AttestationRecord(
            123,
            Collections.emptyList(),
            null,
            new Bitfield(BytesValue.fromHexString("abcdef45").getArrayUnsafe()),
            null,
            12412L,
            12400L,
            DEFAULT_SIG);
    sszSerializer.encode(expected3);
  }

  @Test(expected = NullPointerException.class)
  public void nullListTest() {
    AttestationRecord expected4 =
        new AttestationRecord(
            123,
            null,
            DEFAULT_HASH,
            new Bitfield(BytesValue.fromHexString("abcdef45").getArrayUnsafe()),
            DEFAULT_HASH,
            12412L,
            12400L,
            DEFAULT_SIG);
    sszSerializer.encode(expected4);
  }

  /**
   * Checks that we build objects with {@link SSZSerializer} in the same way as Consensys's {@link
   * SSZ}
   */
  @Test
  public void shouldWorkLikeCavaWithObjects() {
    Bytes bytes =
        fromHexString(
            "0x03000000426F62046807B7711F010000000000000000000000000000000000000000000000000000");
    SomeObject readObject =
        SSZ.decode(bytes, r -> new SomeObject(r.readString(), r.readInt8(), r.readBigInteger(256)));

    assertEquals("Bob", readObject.name);
    assertEquals(4, readObject.number);
    assertEquals(BigInteger.valueOf(1234563434344L), readObject.longNumber);

    // Now try the same with new SSZSerializer
    SomeObject readObjectAuto =
        (SomeObject) sszSerializer.decode(bytes.toArrayUnsafe(), SomeObject.class);
    assertEquals("Bob", readObjectAuto.name);
    assertEquals(4, readObjectAuto.number);
    assertEquals(BigInteger.valueOf(1234563434344L), readObjectAuto.longNumber);
    // and finally check it backwards
    assertArrayEquals(bytes.toArrayUnsafe(), sszSerializer.encode(readObjectAuto));
  }

  /** Checks that we could handle list placed inside another list */
  @Ignore("Implement me!")
  @Test
  public void shouldHandleListList() {
    List<String> list1 = new ArrayList<>();
    list1.add("aa");
    list1.add("bb");
    List<String> list2 = new ArrayList<>();
    list1.add("cc");
    list1.add("dd");
    List<List<String>> listOfLists = new ArrayList<>();
    listOfLists.add(list1);
    listOfLists.add(list2);
    ListListObject expected = new ListListObject(listOfLists);
    byte[] encoded = sszSerializer.encode(expected);
    ListListObject actual = sszSerializer.decode(encoded, ListListObject.class);

    assertEquals(expected, actual);
  }

  @SSZSerializable
  public static class SomeObject {
    private final String name;

    @org.ethereum.beacon.ssz.annotation.SSZ(type = "uint8")
    private final int number;

    @org.ethereum.beacon.ssz.annotation.SSZ(type = "uint256")
    private final BigInteger longNumber;

    public SomeObject(String name, int number, BigInteger longNumber) {
      this.name = name;
      this.number = number;
      this.longNumber = longNumber;
    }

    public String getName() {
      return name;
    }

    public int getNumber() {
      return number;
    }

    public BigInteger getLongNumber() {
      return longNumber;
    }
  }

  @SSZSerializable
  public static class ListListObject {
    private final List<List<String>> names;

    public ListListObject(List<List<String>> names) {
      this.names = names;
    }

    public List<List<String>> getNames() {
      return names;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ListListObject that = (ListListObject) o;
      return names.equals(that.names);
    }
  }

  @SSZSerializable(serializeAs = UInt64.class)
  public static class Child extends UInt64 {
    public Child(UInt64 b) {
      super(b);
    }
  }

  @SSZSerializable
  public static class Wrapper {
    @org.ethereum.beacon.ssz.annotation.SSZ
    public Child c1;

    @org.ethereum.beacon.ssz.annotation.SSZ
    public List<Child> c2;

    @org.ethereum.beacon.ssz.annotation.SSZ
    public Child[] c3;

    public Wrapper(Child c1, List<Child> c2, Child[] c3) {
      this.c1 = c1;
      this.c2 = c2;
      this.c3 = c3;
    }

    @Override
    public boolean equals(Object o) {
      Wrapper wrapper = (Wrapper) o;
      if (c1 != null ? !c1.equals(wrapper.c1) : wrapper.c1 != null) {return false;}
      if (c2 != null ? !c2.equals(wrapper.c2) : wrapper.c2 != null) {return false;}
      return Arrays.equals(c3, wrapper.c3);
    }
  }

  @Test
  public void serializeAsTest1() {
    Wrapper w = new Wrapper(
        new Child(UInt64.valueOf(1)),
        Arrays.asList(
            new Child(UInt64.valueOf(2)),
            new Child(UInt64.valueOf(3))
        ),
        new Child[] {
            new Child(UInt64.valueOf(4)),
            new Child(UInt64.valueOf(5))
        });

    SSZSerializer ssz = new SSZBuilder()
        .addBasicCodecs(new UIntCodec())
        .buildSerializer();

    byte[] bytes = ssz.encode(w);

    Wrapper w1 = ssz.decode(bytes, Wrapper.class);

    Assert.assertEquals(w, w1);
  }

  @SSZSerializable
  public static class Base1 {
    @org.ethereum.beacon.ssz.annotation.SSZ
    public int a;
    @org.ethereum.beacon.ssz.annotation.SSZ
    public String b;

    public Base1(int a, String b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public boolean equals(Object o) {
      Base1 base = (Base1) o;
      if (a != base.a) {return false;}
      return b != null ? b.equals(base.b) : base.b == null;
    }
  }

  @SSZSerializable(serializeAs = UInt64.class)
  public static class Child1 extends Base1 {
    public Child1(Base1 b) {
      super(b.a, b.b);
    }
  }

  @SSZSerializable
  public static class Wrapper1 {
    @org.ethereum.beacon.ssz.annotation.SSZ
    public Child1 c1;

    @org.ethereum.beacon.ssz.annotation.SSZ
    public List<Child1> c2;

    @org.ethereum.beacon.ssz.annotation.SSZ
    public Child1[] c3;

    @Override
    public boolean equals(Object o) {
      Wrapper1 wrapper = (Wrapper1) o;
      if (c1 != null ? !c1.equals(wrapper.c1) : wrapper.c1 != null) {return false;}
      if (c2 != null ? !c2.equals(wrapper.c2) : wrapper.c2 != null) {return false;}
      return Arrays.equals(c3, wrapper.c3);
    }
  }


  @Ignore
  @Test
  public void serializeAsTest2() {
    Wrapper1 w = new Wrapper1();
    w.c1 = new Child1(new Base1(1, "a"));
    w.c2 = Arrays.asList(
            new Child1(new Base1(2, "b")),
            new Child1(new Base1(3, "c"))
          );
    w.c3 = new Child1[] {
            new Child1(new Base1(4, "d")),
            new Child1(new Base1(5, "e"))
          };

    SSZSerializer ssz = sszSerializer;

    byte[] bytes = ssz.encode(w);

    Wrapper1 w1 = ssz.decode(bytes, Wrapper1.class);

    Assert.assertEquals(w, w1);
  }

}

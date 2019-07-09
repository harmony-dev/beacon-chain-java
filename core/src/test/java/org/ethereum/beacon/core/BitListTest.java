package org.ethereum.beacon.core;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.types.Bitlist;
import org.ethereum.beacon.core.types.Bitvector;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZHasher;
import org.ethereum.beacon.ssz.SSZSerializeException;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.junit.Assert.assertEquals;

public class BitListTest {

  @SSZSerializable
  public static class Container4 {
    @SSZ
    int a1;
    @SSZ(vectorLengthVar = "testSize")
    Bitvector c2;

    public Container4() {
    }

    public Container4(int a1, Bitvector c2) {
      this.a1 = a1;
      this.c2 = c2;
    }

    public int getA1() {
      return a1;
    }

    public void setA1(int a1) {
      this.a1 = a1;
    }

    public Bitvector getC2() {
      return c2;
    }

    public void setC2(Bitvector c2) {
      this.c2 = c2;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Container4 that = (Container4) o;
      return a1 == that.a1 &&
          Objects.equal(c2, that.c2);
    }
  }

  @Test
  public void testBitVector() {
    SSZSerializer serializer = new SSZBuilder()
        .withExternalVarResolver(s -> "testSize".equals(s) ? 8 : null)
        .buildSerializer();

    Container4 c4 = new Container4(
        14, Bitvector.of(8, 0b01011101)
    );

    byte[] bytes = serializer.encode(c4);
    System.out.println(BytesValue.wrap(bytes));

    Container4 res = serializer.decode(bytes, Container4.class);
    assertEquals(c4, res);
    System.out.println(res);
  }

  @SSZSerializable
  public static class Container5 {
    @SSZ
    int a1;
    @SSZ(maxSizeVar = "testSize")
    Bitlist c2;

    public Container5() {
    }

    public Container5(int a1, Bitlist c2) {
      this.a1 = a1;
      this.c2 = c2;
    }

    public int getA1() {
      return a1;
    }

    public void setA1(int a1) {
      this.a1 = a1;
    }

    public Bitlist getC2() {
      return c2;
    }

    public void setC2(Bitlist c2) {
      this.c2 = c2;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Container5 that = (Container5) o;
      return a1 == that.a1 &&
          Objects.equal(c2, that.c2);
    }
  }

  @Test(expected = SSZSerializeException.class)
  public void testBitList1() {
    SSZSerializer serializer = new SSZBuilder()
        .withExternalVarResolver(s -> "testSize".equals(s) ? 4 : null)
        .buildSerializer();

    Bitlist bitlist = Bitlist.of(8, 0b01011101, 1234);
    Container5 c5 = new Container5(
        14, bitlist
    );

    byte[] bytes = serializer.encode(c5);
    System.out.println(BytesValue.wrap(bytes));

    Container5 res = serializer.decode(bytes, Container5.class);
    assertEquals(c5, res);
    System.out.println(res);
  }

  @Test
  public void testBitList2() {
    SSZSerializer serializer = new SSZBuilder()
        .withExternalVarResolver(s -> "testSize".equals(s) ? 4 : null)
        .buildSerializer();

    Bitlist bitlist = Bitlist.of(2, 0b01, 4);
    Container5 c5 = new Container5(
        14, bitlist
    );

    byte[] bytes = serializer.encode(c5);
    System.out.println(BytesValue.wrap(bytes));

    Container5 res = serializer.decode(bytes, Container5.class);
    assertEquals(c5, res);
    System.out.println(res);
  }

  @Test
  public void testBitList3() {
    SSZSerializer serializer = new SSZBuilder()
        .withExternalVarResolver(s -> "testSize".equals(s) ? 100500 : null)
        .buildSerializer();

    Bitlist bitlist = Bitlist.of(8, 0b01011110, 100500);
    Container5 c5 = new Container5(
        14, bitlist
    );

    byte[] bytes = serializer.encode(c5);
    System.out.println(BytesValue.wrap(bytes));

    Container5 res = serializer.decode(bytes, Container5.class);
    assertEquals(c5, res);
    System.out.println(res);
  }

  @Test
  public void testBitVectorHash() {
    SSZHasher hasher = new SSZBuilder()
        .withExternalVarResolver(s -> "testSize".equals(s) ? 4 : null)
        .buildHasher(Hashes::sha256);
    Container4 c4 = new Container4(
        14, Bitvector.of(4, 0b0101)
    );

    BytesValue bytes = BytesValue.wrap(hasher.hash(c4));
    System.out.println(bytes);

    assertEquals("0x1b9e7f18834c9652448ac6738c10d7d48c09d0f26f4c600a06215226bff0a1fe", bytes.toString());
  }

  @Test
  public void testBitListHash() {
    SSZHasher hasher = new SSZBuilder()
        .withExternalVarResolver(s -> "testSize".equals(s) ? 4 : null)
        .buildHasher(Hashes::sha256);
    Bitlist bitlist = Bitlist.of(4, 0b0101, 4);
    Container5 c5 = new Container5(
        14, bitlist
    );

    BytesValue bytes = BytesValue.wrap(hasher.hash(c5));
    System.out.println(bytes);

    assertEquals("0x8374d43e309c1ddbb738b29988f6147a971e66922780bb70e7afb3c6612f76bd", bytes.toString());
  }
}

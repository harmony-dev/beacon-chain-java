package org.ethereum.beacon.ssz;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.ethereum.beacon.ssz.access.SSZListAccessor;
import org.ethereum.beacon.ssz.access.basic.BooleanPrimitive;
import org.ethereum.beacon.ssz.access.basic.BytesPrimitive;
import org.ethereum.beacon.ssz.access.basic.StringPrimitive;
import org.ethereum.beacon.ssz.access.basic.UIntPrimitive;
import org.ethereum.beacon.ssz.access.container.SimpleContainerAccessor;
import org.ethereum.beacon.ssz.access.list.ArrayAccessor;
import org.ethereum.beacon.ssz.access.list.ListAccessor;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.creator.CompositeObjCreator;
import org.ethereum.beacon.ssz.creator.ConstructorObjCreator;
import org.ethereum.beacon.ssz.creator.SettersObjCreator;
import org.ethereum.beacon.ssz.incremental.ObservableComposite;
import org.ethereum.beacon.ssz.incremental.UpdateListener;
import org.ethereum.beacon.ssz.type.AccessorResolverRegistry;
import org.ethereum.beacon.ssz.type.SSZContainerType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.SimpleTypeResolver;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.ssz.visitor.SSZIncrementalHasher;
import org.ethereum.beacon.ssz.visitor.SSZSimpleHasher;
import org.ethereum.beacon.ssz.visitor.SSZSimpleHasher.MerkleTrie;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHost;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SSZTypeTest {

  @SSZSerializable
  public static class Container1 {
    @SSZ private int a1;
    @SSZ(type = "uint64") private int a2;
    @SSZ private boolean b;
    @SSZ List<Integer> c1;
    @SSZ List<List<Integer>> c2;
    @SSZ List<Container2> c3;
    @SSZ private int a3;

    public Container1(int a1, int a2, boolean b, List<Integer> c1,
        List<List<Integer>> c2, List<Container2> c3, int a3) {
      this.a1 = a1;
      this.a2 = a2;
      this.b = b;
      this.c1 = c1;
      this.c2 = c2;
      this.c3 = c3;
      this.a3 = a3;
    }

    public boolean isB() {
      return b;
    }

    public int getA1() {
      return a1;
    }

    public int getA2() {
      return a2;
    }

    public List<Integer> getC1() {
      return c1;
    }

    public List<List<Integer>> getC2() {
      return c2;
    }

    public List<Container2> getC3() {
      return c3;
    }

    public int getA3() {
      return a3;
    }
  }

  @SSZSerializable
  public static class Container2 {
    @SSZ int a1;
    @SSZ Container3 b1;
    @SSZ(vectorSize = "2")
    List<Container3> c1;
    @SSZ(vectorSize = "${testSize}")
    List<Integer> c2;
    @SSZ Container3 b2;
    @SSZ int a2;

    public Container2(int a1, Container3 b1,
        List<Container3> c1, List<Integer> c2, Container3 b2, int a2) {
      this.a1 = a1;
      this.b1 = b1;
      this.c1 = c1;
      this.c2 = c2;
      this.b2 = b2;
      this.a2 = a2;
    }

    public int getA1() {
      return a1;
    }

    public Container3 getB1() {
      return b1;
    }

    public List<Container3> getC1() {
      return c1;
    }

    public List<Integer> getC2() {
      return c2;
    }

    public Container3 getB2() {
      return b2;
    }

    public int getA2() {
      return a2;
    }
  }

  @SSZSerializable
  public static class Container3 {
    @SSZ int a1;
    @SSZ int a2;

    public Container3(int a1, int a2) {
      this.a1 = a1;
      this.a2 = a2;
    }

    public int getA1() {
      return a1;
    }

    public int getA2() {
      return a2;
    }
  }

  String dumpType(SSZType type, String indent) {
    String ret = "";
    ret += indent +  type.toStringHelper() + "\n";
    if (type.isList()) {
      ret += dumpType(((SSZListType) type).getElementType(), indent + "  ");
    }
    if (type.isContainer()) {
      for (SSZType sszType : ((SSZContainerType) type).getChildTypes()) {
        ret += dumpType(sszType, indent + "  ");
      }
    }
    return ret;
  }

  @Test
  public void testTypeResolver1() {
    TypeResolver typeResolver = new SSZBuilder()
            .withExternalVarResolver(s -> "testSize".equals(s) ? 1 : null)
            .getTypeResolver();

    SSZType sszType = typeResolver.resolveSSZType(Container1.class);
    System.out.println(dumpType(sszType, ""));
  }

  @Test
  public void testSerializer1() {
    SSZSerializer serializer = new SSZBuilder()
            .withExternalVarResolver(s -> "testSize".equals(s) ? 3 : null)
            .buildSerializer();

    Container1 c1 = new Container1(
      0x11111111,
      0x22222222,
      true,
      asList(0x1111, 0x2222, 0x3333),
      asList(asList(0x11, 0x22), emptyList(), asList(0x33)),
      asList(
        new Container2(
          0x44444444,
          new Container3(0x5555, 0x6666),
          asList(new Container3(0x7777, 0x8888), new Container3(0x9999, 0xaaaa)),
            asList(0x1111, 0x2222, 0x3333),
          new Container3(0, 0),
          0),
        new Container2(
          0x55555555,
          new Container3(0xbbbb, 0xcccc),
          asList(new Container3(0xdddd, 0xeeee), new Container3(0xffff, 0x1111)),
            asList(0x1111, 0x2222, 0x3333),
          new Container3(0, 0),
          0)),
      0x33333333);

    byte[] bytes = serializer.encode(c1);
    System.out.println(BytesValue.wrap(bytes));

    Container1 res = serializer.decode(bytes, Container1.class);
    System.out.println(res);
  }

  @Test
  public void testSerializer2() {
    SSZSerializer serializer = new SSZBuilder()
        .withExternalVarResolver(s -> "testSize".equals(s) ? 2 : null)
        .buildSerializer();

    Container3 c3 = new Container3(0x5555, 0x6666);

    byte[] bytes = serializer.encode(c3);
    System.out.println(BytesValue.wrap(bytes));

    Container3 res = serializer.decode(bytes, Container3.class);
    System.out.println(res);
  }

  @Test
  public void testSerializer3() {
    SSZSerializer serializer = new SSZBuilder()
        .withExternalVarResolver(s -> "testSize".equals(s) ? 3 : null)
        .buildSerializer();

    Container2 c2 = new Container2(
        0x44444444,
        new Container3(0x5555, 0x6666),
        asList(new Container3(0x7777, 0x8888), new Container3(0x9999, 0xaaaa)),
        asList(0x1111, 0x2222, 0x3333),
        new Container3(0xbbbb, 0xcccc),
        0xdddd);

    byte[] bytes = serializer.encode(c2);
    System.out.println(BytesValue.wrap(bytes));

    Container2 res = serializer.decode(bytes, Container2.class);
    System.out.println(res);
  }

  @SSZSerializable
  public interface Ifc1 {
    @SSZ int getA1();
    @SSZ long getA2();
    int getA3();
  }

  @SSZSerializable
  public static class Impl1 implements Ifc1 {

    @Override
    public int getA1() {
      return 0x1111;
    }

    @Override
    public long getA2() {
      return 0x2222;
    }

    @Override
    public int getA3() {
      return 0x3333;
    }

    public int getA4() {
      return 0x4444;
    }
  }

  @SSZSerializable
  public static class Impl2 extends Impl1 {

    @Override
    public int getA3() {
      return 0x5555;
    }

    public int getA5() {
      return 0x6666;
    }
  }

  @Test
  public void testTypeResolver2() throws Exception {
    SSZBuilder sszBuilder = new SSZBuilder()
        .withExternalVarResolver(s -> "testSize".equals(s) ? 1 : null);
    SSZSerializer serializer = sszBuilder.buildSerializer();

    SSZType sszType = sszBuilder.getTypeResolver().resolveSSZType(Impl2.class);
    System.out.println(dumpType(sszType, ""));

    Assert.assertTrue(sszType instanceof SSZContainerType);
    SSZContainerType containerType = (SSZContainerType) sszType;

    Assert.assertEquals(2, containerType.getChildTypes().size());
    Assert.assertEquals("a1", containerType.getChildTypes().get(0).getTypeDescriptor().name);
    Assert.assertEquals("a2", containerType.getChildTypes().get(1).getTypeDescriptor().name);

    byte[] bytes1 = serializer.encode(new Impl2());
    System.out.println(BytesValue.wrap(bytes1));

    byte[] bytes2 = serializer.encode(new Impl1());
    System.out.println(BytesValue.wrap(bytes2));
    Assert.assertArrayEquals(bytes1, bytes2);
    Assert.assertTrue(BytesValue.wrap(bytes1).toString().contains("1111"));
    Assert.assertTrue(BytesValue.wrap(bytes1).toString().contains("2222"));
  }

  @SSZSerializable
  public static class H1 {
    @SSZ public int a1;
    @SSZ public long a2;
    @SSZ public int a3;
  }

  @SSZSerializable
  public static class H2 {
    @SSZ public int a1;
    @SSZ public long a2;
  }

  @Test
  public void testHashTruncated1() throws Exception {
    SSZHasher hasher = new SSZBuilder().buildHasher(Hashes::keccak256);

    H1 h1 = new H1();
    h1.a1 = 0x1111;
    h1.a2 = 0x2222;
    h1.a3 = 0x3333;

    H2 h2 = new H2();
    h2.a1 = 0x1111;
    h2.a2 = 0x2222;

    byte[] h1h = hasher.hash(h1);
    byte[] h2h = hasher.hash(h2);
    byte[] h1hTrunc = hasher.hashTruncate(h1, H1.class, "");

    Assert.assertArrayEquals(h2h, h1hTrunc);
  }

  @SSZSerializable
  public static class I1 implements ObservableComposite {
    UpdateListener updateListener;

    @SSZ private int a1;
    @SSZ private long a2;
    @SSZ private int a3;

    public I1(int a1, long a2, int a3) {
      this.a1 = a1;
      this.a2 = a2;
      this.a3 = a3;
    }

    @Override
    public UpdateListener getUpdateListener(
        String observerId, Supplier<UpdateListener> listenerFactory) {

      return updateListener != null ? updateListener : (updateListener = listenerFactory.get());
    }

    public int getA1() {
      return a1;
    }

    public long getA2() {
      return a2;
    }

    public int getA3() {
      return a3;
    }

    public void setA1(int a1) {
      this.a1 = a1;
      updateListener.childUpdated(0);
    }

    public void setA2(long a2) {
      this.a2 = a2;
      updateListener.childUpdated(1);
    }

    public void setA3(int a3) {
      this.a3 = a3;
      updateListener.childUpdated(2);
    }
  }

  @Test
  public void testHashIncremental1() throws Exception {
    class CountingHash implements Function<BytesValue, Hash32> {
      int counter = 0;

      @Override
      public Hash32 apply(BytesValue bytesValue) {
        counter++;
        return Hashes.keccak256(bytesValue);
      }
    }
    SSZBuilder sszBuilder = new SSZBuilder();
    TypeResolver typeResolver = sszBuilder.getTypeResolver();

    SSZVisitorHost visitorHost = new SSZVisitorHost();
    SSZSerializer serializer = new SSZSerializer(visitorHost, typeResolver);
    CountingHash countingHashSimp = new CountingHash();
    CountingHash countingHashInc = new CountingHash();
    SSZIncrementalHasher incrementalHasher = new SSZIncrementalHasher(serializer, countingHashInc, 32);
    SSZSimpleHasher simpleHasher = new SSZSimpleHasher(serializer, countingHashSimp, 32);

    I1 i1 = new I1(0x1111, 0x2222, 0x3333);

    {
      MerkleTrie mt0 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, simpleHasher);
      MerkleTrie mt1 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, incrementalHasher);
      Assert.assertEquals(mt0.getFinalRoot(), mt1.getFinalRoot());
    }

    i1.setA1(0x4444);

    {
      countingHashInc.counter = 0;
      countingHashSimp.counter = 0;
      MerkleTrie mt2 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, simpleHasher);
      MerkleTrie mt3 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, incrementalHasher);
      Assert.assertEquals(mt2.getFinalRoot(), mt3.getFinalRoot());
      Assert.assertTrue(countingHashInc.counter < countingHashSimp.counter);

      countingHashInc.counter = 0;
      MerkleTrie mt4 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, incrementalHasher);
      Assert.assertEquals(mt2.getFinalRoot(), mt4.getFinalRoot());
      Assert.assertTrue(countingHashInc.counter  == 0);
    }

    i1.setA2(0x5555);

    {
      countingHashInc.counter = 0;
      countingHashSimp.counter = 0;
      MerkleTrie mt2 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, simpleHasher);
      MerkleTrie mt3 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, incrementalHasher);
      Assert.assertEquals(mt2.getFinalRoot(), mt3.getFinalRoot());
      Assert.assertTrue(countingHashInc.counter < countingHashSimp.counter);
    }

    i1.setA3(0x5555);

    {
      countingHashInc.counter = 0;
      countingHashSimp.counter = 0;
      MerkleTrie mt2 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, simpleHasher);
      MerkleTrie mt3 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, incrementalHasher);
      Assert.assertEquals(mt2.getFinalRoot(), mt3.getFinalRoot());
      Assert.assertTrue(countingHashInc.counter < countingHashSimp.counter);
    }

    i1.setA1(0x6666);
    i1.setA2(0x7777);

    {
      countingHashInc.counter = 0;
      countingHashSimp.counter = 0;
      MerkleTrie mt2 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, simpleHasher);
      MerkleTrie mt3 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, incrementalHasher);
      Assert.assertEquals(mt2.getFinalRoot(), mt3.getFinalRoot());
      Assert.assertTrue(countingHashInc.counter < countingHashSimp.counter);
    }

    i1.setA1(0xaaaa);
    i1.setA2(0xbbbb);
    i1.setA3(0xcccc);

    {
      countingHashInc.counter = 0;
      countingHashSimp.counter = 0;
      MerkleTrie mt2 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, simpleHasher);
      MerkleTrie mt3 = visitorHost
          .handleAny(typeResolver.resolveSSZType(I1.class), i1, incrementalHasher);
      Assert.assertEquals(mt2.getFinalRoot(), mt3.getFinalRoot());
      Assert.assertTrue(countingHashInc.counter == countingHashSimp.counter);
    }
  }
}


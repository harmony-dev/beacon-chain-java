package org.ethereum.beacon.ssz;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.scheme.AccessorResolverRegistry;
import org.ethereum.beacon.ssz.scheme.SSZCompositeType;
import org.ethereum.beacon.ssz.scheme.SSZContainerType;
import org.ethereum.beacon.ssz.scheme.SSZListType;
import org.ethereum.beacon.ssz.scheme.SSZType;
import org.ethereum.beacon.ssz.scheme.SimpleTypeResolver;
import org.ethereum.beacon.ssz.scheme.TypeResolver;
import org.junit.Test;
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
    @SSZ(vectorSize = "3")
    List<Container3> c1;
    @SSZ(vectorSize = "${testSize}")
    List<Integer> c2;
    @SSZ Container3 b2;
    @SSZ int a2;

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
    AccessorResolverRegistry resolverRegistry = new AccessorResolverRegistry();
    TypeResolver typeResolver = new SimpleTypeResolver(resolverRegistry, s -> "testSize".equals(s) ? 2 : null);

    SSZType sszType = typeResolver.resolveSSZType(Container1.class);
    System.out.println(dumpType(sszType, ""));
  }

  @Test
  public void testSerializer1() {
    AccessorResolverRegistry resolverRegistry = new AccessorResolverRegistry();
    TypeResolver typeResolver = new SimpleTypeResolver(resolverRegistry,
        s -> "testSize".equals(s) ? 2 : null);
    SSZSerializer serializer = new SSZSerializer(null, null, null, typeResolver);

    Container1 c1 = new Container1();
    c1.a1 = 0x11111111;
    c1.a2 = 0x22222222;
    c1.b = true;
    c1.c1 = asList(0x1111, 0x2222, 0x3333);
    c1.c2 = asList(asList(0x11, 0x22), emptyList(), asList(0x33));
    c1.c3 = new ArrayList<>();
    Container2 c2_1 = new Container2();
    c2_1.a1 = 0x44444444;
    c2_1.b1 = new Container3(0x5555, 0x6666);
    c2_1.b2 = new Container3(0, 0);
    c2_1.c1 = asList(new Container3(0x7777, 0x8888), new Container3(0x9999, 0xaaaa));
    c2_1.c2 = emptyList();
    c1.c3.add(c2_1);
    Container2 c2_2 = new Container2();
    c2_2.a1 = 0x55555555;
    c2_2.b1 = new Container3(0xbbbb, 0xcccc);
    c2_2.b2 = new Container3(0, 0);
    c2_2.c1 = asList(new Container3(0xdddd, 0xeeee), new Container3(0xffff, 0x1111));
    c2_2.c2 = emptyList();
    c1.c3.add(c2_2);
    c1.a3 = 0x33333333;

    byte[] bytes = serializer.encode(c1);
    System.out.println(BytesValue.wrap(bytes));
  }
}

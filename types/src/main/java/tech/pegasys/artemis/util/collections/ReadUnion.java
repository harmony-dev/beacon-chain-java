package tech.pegasys.artemis.util.collections;

import javax.annotation.Nullable;

public interface ReadUnion {

  final class Null {}

  int getTypeIndex();

  @Nullable
  <C> C getValue();

  default <C> C getValueSafe(int typeIndex) {
    if (typeIndex != getTypeIndex()) {
      throw new IllegalStateException("Union type index (" + getTypeIndex() + ") and requested value index (" + typeIndex + ") not match");
    }
    return getValue();
  }

  interface GenericTypedUnion extends ReadUnion {}

  interface U2<P1, P2> extends GenericTypedUnion {
    default P1 getMember1() {
      return getValueSafe(0);
    }
    default P2 getMember2() {
      return getValueSafe(1);
    }
    static <P1, P2> U2<P1, P2> create() {
      return new UnionImpl();
    }
  }

  interface U3<P1, P2, P3> extends U2<P1, P2> {
    default P3 getMember3() {
      return getValueSafe(2);
    }
    static <P1, P2, P3> U3<P1, P2, P3> create() {
      return new UnionImpl();
    }
  }

  interface U4<P1, P2, P3, P4> extends U3<P1, P2, P3> {
    default P4 getMember4() {
      return getValueSafe(3);
    }
    static <P1, P2, P3, P4> U4<P1, P2, P3, P4> create() {
      return new UnionImpl();
    }
  }

  interface U5<P1, P2, P3, P4, P5> extends U4<P1, P2, P3, P4> {
    default P5 getMember5() {
      return getValueSafe(4);
    }
    static <P1, P2, P3, P4, P5> U5<P1, P2, P3, P4, P5> create() {
      return new UnionImpl();
    }
  }
}

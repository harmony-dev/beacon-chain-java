package tech.pegasys.artemis.util.collections;

import javax.annotation.Nullable;

public interface WriteUnion extends ReadUnion {

  <C> void setValue(int index, @Nullable C value);

//  default <C> void setValue(@Nullable C value) {
//    if (value == null) {
//      if (!hasNullType()) {
//        throw new IllegalArgumentException("This union has no null type");
//      }
//      setValue(0, null);
//    } else {
//      int idx = -1;
//      for (int i = 0; i < getMembersCount(); i++) {
//        if (getMemberType(i).isAssignableFrom(value.getClass())) {
//          if (idx >= 0) {
//            throw new IllegalArgumentException(
//                "Ambiguity when assigning Union value. At least two type indices suit (" + idx + ", " + i + ") for value " + value);
//          }
//          idx = i;
//        }
//      }
//      setValue(idx, value);
//    }
//  }

  interface U2<P1, P2> extends WriteUnion, ReadUnion.U2<P1, P2> {
    default void setMember1(P1 val) {
      setValue(0, val);
    }
    default void setMember2(P2 val) {
      setValue(1, val);
    }
    static <P1, P2> WriteUnion.U2<P1, P2> create() {
      return new UnionImpl();
    }
  }
  interface U3<P1, P2, P3> extends U2<P1, P2>, ReadUnion.U3<P1, P2, P3> {
    default void setMember3(P3 val) {
      setValue(2, val);
    }
    static <P1, P2, P3> WriteUnion.U3<P1, P2, P3> create() {
      return new UnionImpl();
    }
  }
  interface U4<P1, P2, P3, P4> extends U3<P1, P2, P3>, ReadUnion.U4<P1, P2, P3, P4> {
    default void setMember4(P4 val) {
      setValue(3, val);
    }
    static <P1, P2, P3, P4> WriteUnion.U4<P1, P2, P3, P4> create() {
      return new UnionImpl();
    }
  }
  interface U5<P1, P2, P3, P4, P5> extends U4<P1, P2, P3, P4>, ReadUnion.U5<P1, P2, P3, P4, P5> {
    default void setMember5(P5 val) {
      setValue(4, val);
    }
    static <P1, P2, P3, P4, P5> WriteUnion.U5<P1, P2, P3, P4, P5> create() {
      return new UnionImpl();
    }
  }
}

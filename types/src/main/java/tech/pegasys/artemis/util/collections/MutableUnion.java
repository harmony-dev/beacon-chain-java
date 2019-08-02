package tech.pegasys.artemis.util.collections;

import javax.annotation.Nullable;

/**
 * See {@link Union} docs for details
 */
public interface MutableUnion extends Union {

  /**
   * Sets the value and the typeIndex of this Union
   */
  <C> void setValue(int typeIndex, @Nullable C value);

  interface U2<P1, P2> extends MutableUnion, Union.U2<P1, P2> {
    default void setMember1(P1 val) {
      setValue(0, val);
    }
    default void setMember2(P2 val) {
      setValue(1, val);
    }
    static <P1, P2> MutableUnion.U2<P1, P2> create() {
      return new GenericUnionImpl();
    }
  }
  interface U3<P1, P2, P3> extends U2<P1, P2>, Union.U3<P1, P2, P3> {
    default void setMember3(P3 val) {
      setValue(2, val);
    }
    static <P1, P2, P3> MutableUnion.U3<P1, P2, P3> create() {
      return new GenericUnionImpl();
    }
  }
  interface U4<P1, P2, P3, P4> extends U3<P1, P2, P3>, Union.U4<P1, P2, P3, P4> {
    default void setMember4(P4 val) {
      setValue(3, val);
    }
    static <P1, P2, P3, P4> MutableUnion.U4<P1, P2, P3, P4> create() {
      return new GenericUnionImpl();
    }
  }
  interface U5<P1, P2, P3, P4, P5> extends U4<P1, P2, P3, P4>, Union.U5<P1, P2, P3, P4, P5> {
    default void setMember5(P5 val) {
      setValue(4, val);
    }
    static <P1, P2, P3, P4, P5> MutableUnion.U5<P1, P2, P3, P4, P5> create() {
      return new GenericUnionImpl();
    }
  }
}

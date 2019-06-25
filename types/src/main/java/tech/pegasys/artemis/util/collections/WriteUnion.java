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

  interface Union2<P1, P2> extends WriteUnion {
  }

  interface Union3<P1, P2, P3> extends WriteUnion {
  }
}

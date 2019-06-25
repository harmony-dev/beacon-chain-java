package tech.pegasys.artemis.util.collections;

import javax.annotation.Nullable;

public interface ReadUnion {

  final class Null {}

//  int getMembersCount();
//
//  Class<?> getMemberType(int index);


  int getTypeIndex();

  @Nullable
  <C> C getValue();

//  default boolean isNull() {
//    return getMemberType(getTypeIndex()) == Null.class;
//  }
//
//  default boolean hasNullType() {
//    return getMemberType(0) == Null.class;
//  }
}

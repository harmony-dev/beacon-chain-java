package tech.pegasys.artemis.util.collections;

import java.util.Collection;
import java.util.stream.Stream;
import tech.pegasys.artemis.util.uint.UInt24;

public interface ReadList<IndexType extends Number, ValueType> extends Iterable<ValueType> {

  static <ValueType> ReadList<UInt24, ValueType> emptyList() {
    return new ListImpl<>(UInt24::valueOf);
  }

  static <ValueType> ReadList<UInt24, ValueType> createUInt24(Collection<ValueType> source) {
    return new ListImpl<>(source, UInt24::valueOf);
  }

  IndexType size();

  ValueType get(IndexType index);

  ListImpl<IndexType, ValueType> subList(IndexType fromIndex, IndexType toIndex);

  WriteList<IndexType, ValueType> createMutableCopy();

  Stream<ValueType> stream();

  default boolean isEmpty() {
    return size().longValue() == 0;
  }

}

package tech.pegasys.artemis.util.collections;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tech.pegasys.artemis.util.uint.UInt24;

public interface ReadList<IndexType extends Number, ValueType> extends Iterable<ValueType> {

  IndexType size();

  ValueType get(IndexType index);

  ReadList<IndexType, ValueType> subList(IndexType fromIndex, IndexType toIndex);

  WriteList<IndexType, ValueType> createMutableCopy();

  Stream<ValueType> stream();

  default boolean isEmpty() {
    return size().longValue() == 0;
  }

  default List<ValueType> listCopy() {
    return stream().collect(Collectors.toList());
  }
}

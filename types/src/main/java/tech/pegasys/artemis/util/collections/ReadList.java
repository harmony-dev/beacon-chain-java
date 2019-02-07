package tech.pegasys.artemis.util.collections;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  default ReadList<IndexType, ValueType> intersection(ReadList<IndexType, ValueType> other) {
    WriteList<IndexType, ValueType> ret = createMutableCopy();
    ret.retainAll(other);
    return ret;
  }

  static int sizeOf(Iterable<?> iterable) {
    if (iterable instanceof ReadList) {
      return ((ReadList) iterable).size().intValue();
    } else if (iterable instanceof List) {
      return ((List) iterable).size();
    } else {
      long size = iterable.spliterator().getExactSizeIfKnown();
      if (size >= 0) {
        return (int) size;
      }
      int counter = 0;
      Iterator<?> iterator = iterable.iterator();
      while (iterator.hasNext()) {
        counter++;
        iterator.next();
      }
      return counter;
    }
  }
}

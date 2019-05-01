package tech.pegasys.artemis.util.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface ReadVector<IndexType extends Number, ValueType>
    extends ReadList<IndexType, ValueType> {

  /** Wraps with creating of new vector */
  static <IndexType extends Number, ValueType> ReadVector<IndexType, ValueType> wrap(
      List<ValueType> srcList, Function<Integer, IndexType> indexConverter) {
    return ListImpl.wrap(new ArrayList<>(srcList), indexConverter);
  }

  default ReadVector<IndexType, ValueType> vectorCopy() {
    ReadVector<IndexType, ValueType> res = createMutableCopy();
    return res;
  }

}

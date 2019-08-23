package tech.pegasys.artemis.util.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface ReadVector<IndexType extends Number, ValueType>
    extends ReadList<IndexType, ValueType> {

  /** Wraps with creating of new vector */
  static <IndexType extends Number, ValueType> ReadVector<IndexType, ValueType> wrap(
      List<ValueType> srcList, Function<Integer, IndexType> indexConverter) {
    return ListImpl.wrap(new ArrayList<>(srcList), indexConverter, true);
  }

  /** Wraps with verifying of length and creating new vector */
  static <IndexType extends Number, ValueType> ReadVector<IndexType, ValueType> wrap(
      List<ValueType> srcList, Function<Integer, IndexType> indexConverter, int vectorLength) {
    assert srcList.size() == vectorLength;
    return ListImpl.wrap(new ArrayList<>(srcList), indexConverter, true);
  }

  default ReadVector<IndexType, ValueType> vectorCopy() {
    ReadVector<IndexType, ValueType> res = createMutableCopy();
    return res;
  }
}

package tech.pegasys.artemis.util.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import tech.pegasys.artemis.util.uint.UInt24;

public interface WriteList<IndexType extends Number, ValueType>
    extends WriteVector<IndexType, ValueType> {

  static <IndexType extends Number, ValueType> WriteList<IndexType, ValueType>
      wrap(List<ValueType> srcList, Function<Integer, IndexType> indexConverter) {
    return ListImpl.wrap(srcList, indexConverter);
  }

  static <IndexType extends Number, ValueType> WriteList<IndexType, ValueType>
      create(Function<Integer, IndexType> indexConverter) {
    return new ListImpl<>(indexConverter);
  }

  boolean add(ValueType valueType);

  boolean remove(ValueType o);

  boolean addAll(@NotNull Collection<? extends ValueType> c);

  boolean addAll(IndexType index, @NotNull Collection<? extends ValueType> c);

  default void replaceAll(@NotNull Collection<? extends ValueType> c) {
    this.clear();
    this.addAll(c);
  }

  void sort(Comparator<? super ValueType> c);

  void clear();

  ValueType set(IndexType index, ValueType element);

  void add(IndexType index, ValueType element);

  ValueType remove(IndexType index);

  void retainAll(ReadList<IndexType, ValueType> other);

  ReadList<IndexType, ValueType> createImmutableCopy();

  default ValueType update(IndexType index, Function<ValueType, ValueType> updater) {
    ValueType newValue = updater.apply(get(index));
    set(index, newValue);
    return newValue;
  }

  default void remove(Predicate<ValueType> removeFilter) {
    List<ValueType> copy = listCopy();
    clear();
    for (ValueType val : copy) {
      if (!removeFilter.test(val)) {
        add(val);
      }
    }
  }
}

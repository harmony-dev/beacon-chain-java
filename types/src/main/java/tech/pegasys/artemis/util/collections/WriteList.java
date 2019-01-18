package tech.pegasys.artemis.util.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import tech.pegasys.artemis.util.uint.UInt24;

public interface WriteList<IndexType extends Number, ValueType>
    extends ReadList<IndexType, ValueType> {

  static <ValueType> WriteList<UInt24, ValueType> createUInt24() {
    return new ListImpl<>(UInt24::valueOf);
  }

  static <ValueType> WriteList<UInt24, ValueType> createUInt24(Collection<ValueType> source) {
    return new ListImpl<>(source, UInt24::valueOf);
  }

  boolean add(ValueType valueType);

  boolean remove(ValueType o);

  boolean addAll(@NotNull Collection<? extends ValueType> c);

  boolean addAll(IndexType index, @NotNull Collection<? extends ValueType> c);

  void sort(Comparator<? super ValueType> c);

  void clear();

  ValueType set(IndexType index, ValueType element);

  void add(IndexType index, ValueType element);

  ValueType remove(IndexType index);

  ReadList<IndexType, ValueType> createImmutableCopy();

  default ValueType update(IndexType index, Function<ValueType, ValueType> updater) {
    ValueType newValue = updater.apply(get(index));
    set(index, newValue);
    return newValue;
  }
}

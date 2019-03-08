package tech.pegasys.artemis.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

class ListImpl<IndexType extends Number, ValueType>
    implements WriteList<IndexType, ValueType> {

  static <IndexType extends Number, ValueType> WriteList<IndexType, ValueType>
      wrap(List<ValueType> backedList, Function<Integer, IndexType> indexConverter) {
    return new ListImpl<>(backedList, indexConverter);
  }

  private final List<ValueType> backedList;
  private final Function<Integer, IndexType> indexConverter;

  private ListImpl(List<ValueType> backedList,
      Function<Integer, IndexType> indexConverter) {
    this.backedList = backedList;
    this.indexConverter = indexConverter;
  }

  ListImpl(Collection<ValueType> source,
      Function<Integer, IndexType> indexConverter) {
    this(new ArrayList<>(source), indexConverter);
  }

  ListImpl(Function<Integer, IndexType> indexConverter) {
    this(new ArrayList<>(), indexConverter);
  }

  @Override
  public IndexType size() {
    return indexConverter.apply(backedList.size());
  }

  @Override
  public ValueType get(IndexType index) {
    return backedList.get(index.intValue());
  }

  @Override
  public ListImpl<IndexType, ValueType> subList(IndexType fromIndex, IndexType toIndex) {
    return new ListImpl<>(backedList.subList(fromIndex.intValue(), toIndex.intValue()), indexConverter);
  }

  @Override
  public WriteList<IndexType, ValueType> createMutableCopy() {
    return new ListImpl<>(backedList, indexConverter);
  }

  @NotNull
  @Override
  public Iterator<ValueType> iterator() {
    return backedList.iterator();
  }

  @Override
  public Stream<ValueType> stream() {
    return backedList.stream();
  }

  @Override
  public boolean add(ValueType valueType) {
    return backedList.add(valueType);
  }

  @Override
  public boolean remove(ValueType o) {
    return backedList.remove(o);
  }

  @Override
  public boolean addAll(@NotNull Collection<? extends ValueType> c) {
    return backedList.addAll(c);
  }

  @Override
  public boolean addAll(IndexType index, @NotNull Collection<? extends ValueType> c) {
    return backedList.addAll(index.intValue(), c);
  }

  @Override
  public void sort(Comparator<? super ValueType> c) {
    backedList.sort(c);
  }

  @Override
  public void clear() {
    backedList.clear();
  }

  @Override
  public List<ValueType> listCopy() {
    return new ArrayList<>(backedList);
  }

  @Override
  public ValueType set(IndexType index, ValueType element) {
    return backedList.set(index.intValue(), element);
  }

  @Override
  public void add(IndexType index, ValueType element) {
    backedList.add(index.intValue(), element);
  }

  @Override
  public ValueType remove(IndexType index) {
    return backedList.remove(index.intValue());
  }

  @Override
  public void retainAll(ReadList<IndexType, ValueType> other) {
    backedList.retainAll(other.listCopy());
  }

  @Override
  public ReadList<IndexType, ValueType> createImmutableCopy() {
    return new ListImpl<>(backedList, indexConverter);
  }

  @Override
  public boolean equals(Object o) {
    return backedList.equals(o);
  }

  @Override
  public int hashCode() {
    return backedList.hashCode();
  }
}

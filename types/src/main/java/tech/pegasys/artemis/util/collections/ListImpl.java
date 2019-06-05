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

  ListImpl(Collection<ValueType> source,
      Function<Integer, IndexType> indexConverter) {
    this.backedList = new ArrayList<>(source);
    this.indexConverter = indexConverter;
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
  public boolean addAll(@NotNull Iterable<? extends ValueType> c) {
    if (c instanceof Collection) {
      return backedList.addAll((Collection<? extends ValueType>) c);
    } else if (c instanceof ListImpl) {
      return backedList.addAll(((ListImpl<?, ? extends ValueType>) c).backedList);
    } else {
      boolean hasAny = false;
      for (ValueType val : c) {
        backedList.add(val);
        hasAny = true;
      }
      return hasAny;
    }
  }

  @Override
  public boolean addAll(IndexType index, @NotNull Iterable<? extends ValueType> c) {
    if (c instanceof Collection) {
      return backedList.addAll(index.intValue(), (Collection<? extends ValueType>) c);
    } else if (c instanceof ListImpl) {
      return backedList.addAll(index.intValue(), ((ListImpl<?, ? extends ValueType>) c).backedList);
    } else {
      int idx = index.intValue();
      for (ValueType val : c) {
        backedList.add(idx++, val);
      }
      return idx > index.intValue();
    }
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
  public void setAll(ValueType singleValue) {
    for (int i = 0; i < backedList.size(); i++) {
      backedList.set(i, singleValue);
    }
  }

  @Override
  public void setAll(Iterable<ValueType> singleValue) {
    Iterator<ValueType> it = singleValue.iterator();
    int idx = 0;
    while (it.hasNext() && idx < backedList.size()) {
      backedList.set(idx, it.next());
      idx++;
    }
    if (it.hasNext() || idx < backedList.size()) {
      throw new IllegalArgumentException("The sizes of this vector and supplied collection differ");
    }
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
    return backedList.equals(((ListImpl)o).backedList);
  }

  @Override
  public int hashCode() {
    return backedList.hashCode();
  }
}

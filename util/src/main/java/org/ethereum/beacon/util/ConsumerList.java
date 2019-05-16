package org.ethereum.beacon.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ConsumerList<E> extends LinkedList<E> {

  private final LinkedList<E> delegate;
  private final Consumer<E> spillOutConsumer;

  @VisibleForTesting
  final int maxSize;

  private ConsumerList(int maxSize, Consumer<E> spillOutConsumer) {
    checkArgument(maxSize >= 0, "maxSize (%s) must >= 0", maxSize);
    this.delegate = new LinkedList<E>();
    this.maxSize = maxSize;
    this.spillOutConsumer = spillOutConsumer;
  }

  /**
   * Creates and returns a new consumer list that will hold up to {@code maxSize} elements.
   *
   * <p>When {@code maxSize} is zero, elements will be consumed immediately after being added to the
   * queue.
   */
  public static <E> ConsumerList<E> create(int maxSize, Consumer<E> spillOutConsumer) {
    return new ConsumerList<>(maxSize, spillOutConsumer);
  }


  /**
   * Returns the number of additional elements that this list can accept without consuming; zero if
   * the list is currently full.
   *
   * @since 16.0
   */
  public int remainingCapacity() {
    return maxSize - size();
  }

  protected List<E> delegate() {
    return delegate;
  }

  /**
   * Adds the given element to this queue. If the queue is currently full, the element at the head
   * of the queue is consumed to make room.
   *
   * @return {@code true} always
   */
  @Override
  public boolean add(E e) {
    checkNotNull(e); // check before removing
    if (maxSize == 0) {
      spillOutConsumer.accept(e);
      return true;
    }
    if (size() == maxSize) {
      spillOutConsumer.accept(delegate.remove());
    }
    delegate.add(e);
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends E> collection) {
    int size = collection.size();
    if (size >= maxSize) {
      while(!delegate.isEmpty()) {
        spillOutConsumer.accept(delegate.remove());
      }
      int numberToSkip = size - maxSize;
      Iterator<? extends E> iterator = collection.iterator();
      int i = 0;
      while (iterator.hasNext() && i < numberToSkip) {
        spillOutConsumer.accept(iterator.next());
        ++i;
      }
      return Iterables.addAll(this, Iterables.skip(collection, numberToSkip));
    }
    return Iterators.addAll(this, collection.iterator());
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.contains(o);
  }

  @Override
  public E getFirst() {
    return delegate.getFirst();
  }

  @Override
  public E getLast() {
    return delegate.getLast();
  }

  @Override
  public E get(int index) {
    return delegate.get(index);
  }

  @Override
  public Stream<E> stream() {
    return delegate.stream();
  }
}

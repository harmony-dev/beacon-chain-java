package org.ethereum.beacon.core.types;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface TypeIterable<C extends TypeIterable<C>> extends SafeComparable<C>, Iterable<C> {

  C increment();

  C decrement();

  C zeroElement();

  @Override
  default Iterator<C> iterator() {
    return iterateFrom(zeroElement()).iterator();
  }

  default Stream<C> streamFromZero() {
    return streamFrom(zeroElement());
  }

  default Stream<C> streamFrom(C fromNumber) {
    return StreamSupport.stream(iterateFrom(fromNumber).spliterator(), false);
  }

  default Stream<C> streamTo(C toNumber) {
    return StreamSupport.stream(iterateTo(toNumber).spliterator(), false);
  }

  default Iterable<C> iterateFromZero() {
    return iterateFrom(zeroElement());
  }

  default Iterable<C> iterateFrom(C fromNumber) {
    return fromNumber.iterateTo((C) this);
  }

  default Iterable<C> iterateTo(C toNumber) {
    class Iter implements Iterator<C> {
      private C cur;
      private C end;
      private boolean increasing;

      private Iter(C from, C to, boolean increasing) {
        this.increasing = increasing;
        this.cur = from;
        this.end = to;
      }

      @Override
      public boolean hasNext() {
        return increasing ? cur.less(end) : cur.greater(end);
      }

      @Override
      public C next() {
        if (!hasNext()) throw new NoSuchElementException();
        C ret = cur;
        cur = (C) (increasing ? cur.increment() : cur.decrement());
        return ret;
      }
    }
    return () -> new Iter((C) this, toNumber, true);
  }
}

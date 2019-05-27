package org.ethereum.beacon.stream;

import java.util.ArrayList;
import java.util.List;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class RxUtil {


  public static <T> Publisher<T> join(Publisher<T> s1, Publisher<T> s2, int bufferLen) {
    throw new UnsupportedOperationException();
  }

  enum Op {
    ADDED, REMOVED
  }

  public static <T> Flux<List<T>> collect(Publisher<T> addedStream, Publisher<T> removedStream) {
    return Flux.merge(
        Flux.from(addedStream).map(e -> Pair.with(Op.ADDED, e)),
        Flux.from(removedStream).map(e -> Pair.with(Op.REMOVED, e))
    ).scan(new ArrayList<T>(), (arr, op) -> {
      ArrayList<T> ret = new ArrayList<>(arr);
      if (op.getValue0() == Op.ADDED) {
        ret.add(op.getValue1());
      } else {
        ret.remove(op.getValue1());
      }
      return ret;
    });
  }

}

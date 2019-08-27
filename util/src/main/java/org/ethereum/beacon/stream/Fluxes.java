package org.ethereum.beacon.stream;

import java.util.function.Predicate;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public abstract class Fluxes {
  private Fluxes() {}

  public static <T> FluxSplit<T> split(Publisher<T> source, Predicate<T> predicate) {
    return new FluxSplit<>(source, predicate);
  }

  public static final class FluxSplit<T> {
    private final Flux<T> satisfied;
    private final Flux<T> unsatisfied;
    private final Disposable disposable;

    FluxSplit(Publisher<T> source, Predicate<T> predicate) {
      ConnectableFlux<Pair<Boolean, T>> splitter =
          Flux.from(source).map(value -> Pair.with(predicate.test(value), value)).publish();

      this.satisfied = splitter.filter(Pair::getValue0).map(Pair::getValue1);
      this.unsatisfied = splitter.filter(pair -> !pair.getValue0()).map(Pair::getValue1);
      this.disposable = splitter.connect();
    }

    public Flux<T> getSatisfied() {
      return satisfied;
    }

    public Flux<T> getUnsatisfied() {
      return unsatisfied;
    }

    public Disposable getDisposable() {
      return disposable;
    }
  }
}

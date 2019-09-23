package org.ethereum.beacon.stream;

import java.util.function.Predicate;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

/** Various utility methods to work with {@link Flux}. */
public abstract class Fluxes {
  private Fluxes() {}

  /**
   * Given predicate creates a flux split.
   *
   * @param source a source.
   * @param predicate a predicate.
   * @param <T> a kind of source data.
   * @return a flux split.
   */
  public static <T> FluxSplit<T> split(Publisher<T> source, Predicate<T> predicate) {
    return new FluxSplit<>(source, predicate);
  }

  /**
   * A split of some publisher made upon a predicate.
   *
   * <p>Built atop of {@link ConnectableFlux}.
   *
   * @param <T> a kind of source data.
   */
  public static final class FluxSplit<T> {
    private final Flux<T> satisfied;
    private final Flux<T> unsatisfied;
    private final Disposable disposable;

    /**
     * Given source and predicate creates a split.
     *
     * @param source a source publisher.
     * @param predicate a predicate.
     */
    FluxSplit(Publisher<T> source, Predicate<T> predicate) {
      ConnectableFlux<Pair<Boolean, T>> splitter =
          Flux.from(source).map(value -> Pair.with(predicate.test(value), value)).publish();

      this.satisfied = splitter.filter(Pair::getValue0).map(Pair::getValue1);
      this.unsatisfied = splitter.filter(pair -> !pair.getValue0()).map(Pair::getValue1);
      this.disposable = splitter.connect();
    }

    /** @return a flux of data items which predicates with {@code true}. */
    public Flux<T> getSatisfied() {
      return satisfied;
    }

    /** @return a flux of data items which predicates with {@code false}. */
    public Flux<T> getUnsatisfied() {
      return unsatisfied;
    }

    /** @return a disposable for connection between source and outcomes. */
    public Disposable getDisposable() {
      return disposable;
    }
  }
}

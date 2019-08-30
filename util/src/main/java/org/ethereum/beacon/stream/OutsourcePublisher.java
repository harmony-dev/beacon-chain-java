package org.ethereum.beacon.stream;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * An implementation of publisher which could be manually fed with data as a source.
 *
 * <p>Delegates to {@link DirectProcessor} and uses its sink to feed the data.
 *
 * <p><b>Note: </b> DirectProcessor does not coordinate backpressure between its Subscribers and the
 * upstream, but consumes its upstream in an * unbounded manner.
 *
 * @param <T> a kind of data.
 */
public class OutsourcePublisher<T> extends Flux<T> {

  private final DirectProcessor<T> delegate = DirectProcessor.create();
  private final FluxSink<T> out = delegate.sink();

  /**
   * Publishes a new value.
   *
   * @param value a value.
   */
  public void publishOut(T value) {
    out.next(value);
  }

  @Override
  public void subscribe(CoreSubscriber<? super T> actual) {
    delegate.subscribe(actual);
  }
}

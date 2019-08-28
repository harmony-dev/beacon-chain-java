package org.ethereum.beacon.stream;

import javax.annotation.Nonnull;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxProcessor;

/**
 * An abstract processor built atop of reactor abstractions.
 *
 * <p>Delegates to {@link OutsourcePublisher} which it its turn delegates to {@link
 * DirectProcessor}.
 *
 * <p><b>Note: </b> DirectProcessor does not coordinate backpressure between its Subscribers and the
 * upstream, but consumes its upstream in an * unbounded manner.
 *
 * @param <IN> a kind of input data.
 * @param <OUT> a kind of output data.
 */
public abstract class AbstractDelegateProcessor<IN, OUT> extends FluxProcessor<IN, OUT> {

  private final Subscriber subscriber;
  private final OutsourcePublisher<OUT> publisher;

  public AbstractDelegateProcessor() {
    this.subscriber = new Subscriber();
    this.publisher = new OutsourcePublisher<>();
  }

  @Override
  public void onSubscribe(Subscription s) {
    subscriber.onSubscribe(s);
  }

  @Override
  public void onNext(IN in) {
    subscriber.onNext(in);
  }

  @Override
  public void onError(Throwable t) {
    subscriber.onError(t);
  }

  @Override
  public void onComplete() {
    subscriber.onComplete();
  }

  @Override
  public void subscribe(@Nonnull CoreSubscriber<? super OUT> actual) {
    publisher.subscribe(actual);
  }

  private final class Subscriber extends BaseSubscriber<IN> {
    @Override
    protected void hookOnNext(IN value) {
      AbstractDelegateProcessor.this.hookOnNext(value);
    }
  }

  /**
   * Called when there is a new input value.
   *
   * @param value a value.
   */
  protected abstract void hookOnNext(IN value);

  /**
   * Should be called in order to publish a new value.
   *
   * @param value a value.
   */
  protected void publishOut(OUT value) {
    publisher.publishOut(value);
  }
}

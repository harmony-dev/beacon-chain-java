package org.ethereum.beacon.stream;

import javax.annotation.Nonnull;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.FluxProcessor;

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

  protected abstract void hookOnNext(IN value);

  protected void publishOut(OUT value) {
    publisher.publishOut(value);
  }
}

package org.ethereum.beacon.stream;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Scheduler;

public class SimpleProcessor<T> implements Processor<T, T> {
  FluxProcessor<T, T> subscriber;
  FluxSink<T> sink;
  Flux<T> publisher;
  boolean subscribed;

  public SimpleProcessor(Scheduler scheduler, String name) {
    ReplayProcessor<T> processor = ReplayProcessor.cacheLast();
    subscriber = processor;
    sink = subscriber.sink();
    publisher = Flux.from(processor)
        .publishOn(scheduler)
        .onBackpressureError()
        .name(name);
  }

  public SimpleProcessor doOnAnySubscribed(Runnable handler) {
    publisher = publisher.doOnSubscribe(s -> {
      if (!subscribed) {
        subscribed = true;
        handler.run();
      }
    });
    return this;
  }

  public SimpleProcessor doOnNoneSubscribed(Runnable handler) {
    publisher = publisher.doOnCancel(() -> {
      if (subscribed && !subscriber.hasDownstreams()) {
        subscribed = false;
        handler.run();
      }
    });
    return this;
  }

  @Override
  public void subscribe(Subscriber<? super T> subscriber) {
    publisher.subscribe(subscriber);
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    subscriber.onSubscribe(subscription);
  }

  @Override
  public void onNext(T t) {
    sink.next(t);
  }

  @Override
  public void onError(Throwable throwable) {
    sink.error(throwable);
  }

  @Override
  public void onComplete() {
    sink.complete();
  }
}

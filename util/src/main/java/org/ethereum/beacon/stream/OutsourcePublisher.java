package org.ethereum.beacon.stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxSink;

public class OutsourcePublisher<T> implements Publisher<T> {

  private final DirectProcessor<T> delegate = DirectProcessor.create();
  private final FluxSink<T> out = delegate.sink();

  public void publishOut(T value) {
    out.next(value);
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    delegate.subscribe(s);
  }
}

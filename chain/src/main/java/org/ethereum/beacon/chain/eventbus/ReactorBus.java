package org.ethereum.beacon.chain.eventbus;

import java.util.function.Consumer;
import org.ethereum.beacon.schedulers.Schedulers;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxSink;

public class ReactorBus implements EventBus {

  private final DirectProcessor<Event<?>> processor = DirectProcessor.create();
  private final FluxSink<Event<?>> sink = processor.sink();

  public ReactorBus(Schedulers schedulers) {
    processor.publishOn(schedulers.events().toReactor());
  }

  @Override
  public void publish(Event<?> event) {
    sink.next(event);
  }

  @Override
  public <T> void subscribe(Class<? extends Event<T>> type, Consumer<T> consumer) {
    processor.filter(e -> e.getClass().equals(type)).map(e -> (T) e.getValue()).subscribe(consumer);
  }
}

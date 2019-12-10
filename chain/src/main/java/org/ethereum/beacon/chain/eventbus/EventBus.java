package org.ethereum.beacon.chain.eventbus;

import java.util.function.Consumer;
import org.ethereum.beacon.schedulers.Schedulers;

public interface EventBus {

  void publish(Event<?> event);

  <T> void subscribe(Class<? extends Event<T>> type, Consumer<T> consumer);

  static EventBus create(Schedulers schedulers) {
    return new ReactorBus(schedulers);
  }

  interface Event<T> {
    T getValue();
  }
}

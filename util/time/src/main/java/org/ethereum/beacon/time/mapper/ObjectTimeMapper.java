package org.ethereum.beacon.time.mapper;

import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public class ObjectTimeMapper<T> implements TimeMapper {
  private final Function<T, Time> timeFunc;
  private final SimpleProcessor<Time> timeProcessor;

  public ObjectTimeMapper(
      Scheduler scheduler, Publisher<T> objectStream, Function<T, Time> timeFunc) {
    this.timeProcessor = new SimpleProcessor<>(scheduler, "TimeMapper");
    this.timeFunc = timeFunc;
    Flux.from(objectStream).map(this::mapObjectFunc).subscribe(timeProcessor::onNext);
  }

  Time mapObjectFunc(T obj) {
    return timeFunc.apply(obj);
  }

  @Override
  public Publisher<Time> getTimeStream() {
    return timeProcessor;
  }
}

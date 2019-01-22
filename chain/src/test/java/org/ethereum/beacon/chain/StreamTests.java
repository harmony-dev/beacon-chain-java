package org.ethereum.beacon.chain;

import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.junit.Test;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.FluxSink.OverflowStrategy;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

public class StreamTests {

  FluxSink<Long> sink;
  @Test
  public void test1() throws InterruptedException {
    ReplayProcessor<Long> processor = ReplayProcessor.cacheLast();

    Publisher<Long> stream = Flux.from(processor)
        .publishOn(Schedulers.single());

    for (int i = 0; i < 10; i++) {
      processor.onNext((long)i);
    }

    Flux.from(stream)
        .doOnSubscribe(s -> System.out.println("#1: subscribe"))
        .doOnNext(l -> System.out.println("#1: " + l))
        .subscribe();
    Flux.from(stream)
        .doOnSubscribe(s -> System.out.println("#2: subscribe"))
        .doOnNext(l -> System.out.println("#2: " + l))
        .subscribe();

    Thread.sleep(200);

    for (int i = 10; i < 20; i++) {
      processor.onNext((long)i);
      Thread.sleep(20);
    }

    Thread.sleep(1000L);
  }
}

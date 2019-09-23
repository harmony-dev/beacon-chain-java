package org.ethereum.beacon.chain;

import org.junit.jupiter.api.*;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

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

  @Test
  @Disabled
  public void intervalTest1() throws InterruptedException {

    long initDelay = (System.currentTimeMillis() / 10000 + 1) * 10000 - System.currentTimeMillis();
    Flux<Long> interval = Flux.interval(Duration.ofMillis(initDelay), Duration.ofSeconds(10));
    Thread.sleep(2000);

    Disposable subscribe = interval
        .subscribe(l -> System.out.println(l + ": " + System.currentTimeMillis() % 60_000));
    Thread.sleep(20000);

    subscribe.dispose();
    System.out.println("Unsubscribed");
    Thread.sleep(2000);

    interval
        .subscribe(l -> System.out.println(l + ": " + System.currentTimeMillis() % 60_000));
    Thread.sleep(20000);
  }
}

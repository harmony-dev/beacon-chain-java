package org.ethereum.beacon.wire;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.stream.RxUtil;
import org.javatuples.Pair;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;
import reactor.test.StepVerifier.FirstStep;

public class RxTest {

  FluxSink<Integer> addSink;
  FluxSink<Integer> removeSink;

  @Test
  public void test1() {
    Publisher<Integer> addedPeersStream = Flux.create(e -> addSink = e);
    Publisher<Integer> removedPeersStream = Flux.create(e -> removeSink = e);

    Flux<List<Integer>> activeList = RxUtil.collect(addedPeersStream, removedPeersStream);

    activeList.subscribe(l -> System.out.println(l));
    FirstStep<List<Integer>> test = StepVerifier.create(activeList);

    addSink.next(1);

    test.expectNext(new ArrayList<>(asList(1)));

    addSink.next(2);
    addSink.next(3);

    test.expectNext(new ArrayList<>(asList(1, 2, 3)));

    removeSink.next(2);

    test.expectNext(new ArrayList<>(asList(1, 3)));
  }
}

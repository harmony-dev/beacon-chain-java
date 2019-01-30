package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.types.SlotTick;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;
import java.time.Duration;

public class BeaconChainValidatorTest {
  private Publisher<SlotTick> slotTickPublisher = Flux.interval(Duration.ofSeconds(6))
      .map((Long t) -> new SlotTick());
  private Publisher<ObservableBeaconState> recentStates;

  @Before
  public void setup() {
    this.recentStates = Flux.merge(
        Flux.just(stateOfSlot(11)).delayElements(Duration.ofMillis(6000)),
        Flux.just(stateOfSlot(12)).delayElements(Duration.ofMillis(9500)),
        Flux.just(stateOfSlot(21)).delayElements(Duration.ofMillis(12000)),
        Flux.just(stateOfSlot(22)).delayElements(Duration.ofMillis(15500)),
        Flux.just(stateOfSlot(23)).delayElements(Duration.ofMillis(15700)),
        Flux.just(stateOfSlot(31)).delayElements(Duration.ofMillis(18000)),
        Flux.just(stateOfSlot(32)).delayElements(Duration.ofMillis(22000))
    ).cache(1);

    this.slotTickPublisher.subscribe(new Subscriber<SlotTick>() {
      @Override
      public void onSubscribe(Subscription s) {
        s.request(10);
      }

      @Override
      public void onNext(SlotTick slotTick) {
        System.out.println("We got a tick!");
        Flux.from(recentStates).delaySubscription(Duration.ofSeconds(4)).next().subscribe(BeaconChainValidatorTest::process);
      }

      @Override
      public void onError(Throwable t) {
      }

      @Override
      public void onComplete() {
      }
    });
  }

  private static void process(ObservableBeaconState state) {
    System.out.println("Got state for processing with head for slot [" + state.getHead().getSlot() + "]");
  }

  /**
   * 6 seconds: state11
   * 9.5 seconds: state12
   * 12 seconds: state21
   * 15.5 seconds: state22
   * 15.7 seconds: state23
   * 18 seconds: state31
   * 22 seconds: state32
   *
   * expected: process task with state12, state23, state31, state32, state32, state32...
   *
   * @throws InterruptedException
   */
  @Test
  @Ignore("Sandbox test, not fast")
  public void test1() throws InterruptedException {
    Flux.from(recentStates).subscribe(state -> System.out.println("New state with #" + state.getHead().getSlot()));
    Thread.sleep(50000);
  }

  private ObservableBeaconState stateOfSlot(int slot) {
    return new ObservableBeaconState(
            BeaconBlock.Builder.createEmpty()
                .withSlot(UInt64.valueOf(slot))
                .withParentRoot(Hash32.ZERO)
                .withStateRoot(Hash32.ZERO)
                .withRandaoReveal(Bytes96.ZERO)
                .withEth1Data(Eth1Data.EMPTY)
                .withSignature(Bytes96.ZERO)
                .withBody(BeaconBlockBody.EMPTY)
                .build(),
            null,
            null);
  }

  private void shouldRunOnce(ObservableBeaconState state) {
    System.out.println("Got state for block with slot [" + state.getHead().getSlot() + "]");
  }
}

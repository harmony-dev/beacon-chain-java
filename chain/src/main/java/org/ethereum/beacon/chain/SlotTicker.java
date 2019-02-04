package org.ethereum.beacon.chain;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.util.uint.UInt64;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Generator of {@link SlotNumber}
 *
 * <p>The only confidential source of slot start. All services with behavior that depends on slot
 * start must subscribe to this service and use values from it
 */
public class SlotTicker implements Ticker<SlotNumber> {
  private final SpecHelpers specHelpers;
  private final Time genesisTime;
  private final Supplier<UInt64> accurateTime;
  private UInt64 slot;

  private final DirectProcessor<SlotNumber> slotSink = DirectProcessor.create();
  private final Publisher<SlotNumber> slotStream =
      Flux.from(slotSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("SlotTicker.slot");

  private static final long MILLIS_IN_SECOND = Duration.ofSeconds(1).toMillis();

  public SlotTicker(SpecHelpers specHelpers, BeaconState state) {
    this.specHelpers = specHelpers;
    this.accurateTime = specHelpers.accurateTimeMillis();
    this.genesisTime = state.getGenesisTime();
  }

  /** Execute to start {@link SlotNumber} propagation */
  @Override
  public void start() {
    SlotNumber genesisSlot = specHelpers.getChainSpec().getGenesisSlot();
    Time slotDuration = specHelpers.getChainSpec().getSlotDuration();
    UInt64 slotDelta =
        accurateTime
            .get()
            .dividedBy(UInt64.valueOf(MILLIS_IN_SECOND))
            .minus(genesisTime)
            .dividedBy(slotDuration);
    UInt64 nextSlotStartTime = genesisTime.plus(slotDelta.increment()).times(slotDuration);
    this.slot = genesisSlot.plus(slotDelta);
    UInt64 delay = nextSlotStartTime.times(MILLIS_IN_SECOND).minus(accurateTime.get());
    Flux.interval(Duration.ofSeconds(slotDuration.getIntValue()))
        .delaySubscription(Duration.ofMillis(delay.longValue()))
        .subscribe(
            tick -> {
              this.slot = slot.increment();
              slotSink.onNext(new SlotNumber(slot));
            });
  }

  /** Stream fires current slot number at slot time start */
  @Override
  public Publisher<SlotNumber> getTickerStream() {
    return slotStream;
  }
}

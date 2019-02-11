package org.ethereum.beacon.chain;

import java.time.Duration;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.Times;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Generator of {@link SlotNumber}
 *
 * <p>The only confidential source of slot start. All services with behavior that depends on slot
 * start must subscribe to this service and use values from it
 */
public class SlotTicker implements Ticker<SlotNumber> {
  private final SpecHelpers specHelpers;
  private final BeaconState state;

  private final DirectProcessor<SlotNumber> slotSink = DirectProcessor.create();
  private final Publisher<SlotNumber> slotStream =
      Flux.from(slotSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("SlotTicker.slot");

  private SlotNumber startSlot;

  private Scheduler scheduler;

  public SlotTicker(SpecHelpers specHelpers, BeaconState state) {
    this.specHelpers = specHelpers;
    this.state = state;
  }

  /** Execute to start {@link SlotNumber} propagation */
  @Override
  public void start() {
    this.scheduler = Schedulers.newSingle("slot-ticker");

    SlotNumber nextSlot = specHelpers.get_current_slot(state).increment();
    Time period = specHelpers.getChainSpec().getSlotDuration();
    startImpl(nextSlot, period, scheduler);
  }

  @Override
  public void stop() {
    assert scheduler != null;
    scheduler.dispose();
  }

  private void startImpl(SlotNumber startSlot, Time period, Scheduler scheduler) {
    this.startSlot = startSlot;

    Time startSlotTime = specHelpers.get_slot_start_time(state, startSlot);
    long delayMillis =
        Math.max(0, startSlotTime.getMillis().getValue() - Times.currentTimeMillis().getValue());
    Flux.interval(Duration.ofMillis(delayMillis), Duration.ofSeconds(period.getValue()), scheduler)
        .subscribe(tick -> slotSink.onNext(this.startSlot.plus(tick)));
  }

  /** Stream fires current slot number at slot time start */
  @Override
  public Publisher<SlotNumber> getTickerStream() {
    return slotStream;
  }
}

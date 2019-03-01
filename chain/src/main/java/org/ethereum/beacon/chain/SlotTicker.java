package org.ethereum.beacon.chain;

import java.time.Duration;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

/**
 * Generator of {@link SlotNumber}
 *
 * <p>The only confidential source of slot start. All services with behavior that depends on slot
 * start must subscribe to this service and use values from it
 */
public class SlotTicker implements Ticker<SlotNumber> {
  private final SpecHelpers specHelpers;
  private final BeaconState state;

  private final Schedulers schedulers;
  private final DirectProcessor<SlotNumber> slotSink = DirectProcessor.create();
  private final Publisher<SlotNumber> slotStream;

  private SlotNumber startSlot;

  private Scheduler scheduler;

  public SlotTicker(SpecHelpers specHelpers, BeaconState state, Schedulers schedulers) {
    this.specHelpers = specHelpers;
    this.state = state;
    this.schedulers = schedulers;

    slotStream = Flux.from(slotSink)
            .publishOn(this.schedulers.reactorEvents())
            .onBackpressureError()
            .name("SlotTicker.slot");
  }

  /** Execute to start {@link SlotNumber} propagation */
  @Override
  public void start() {
    this.scheduler = schedulers.newSingleThreadDaemon("slot-ticker");

    SlotNumber nextSlot = specHelpers.get_current_slot(state).increment();
    Time period = specHelpers.getChainSpec().getSecondsPerSlot();
    startImpl(nextSlot, period, scheduler);
  }

  @Override
  public void stop() {}

  private void startImpl(SlotNumber startSlot, Time period, Scheduler scheduler) {
    this.startSlot = startSlot;

    Time startSlotTime = specHelpers.get_slot_start_time(state, startSlot);
    long delayMillis =
        Math.max(0, startSlotTime.getMillis().getValue() - schedulers.getCurrentTime());
    Flux.interval(
            Duration.ofMillis(delayMillis),
            Duration.ofSeconds(period.getValue()),
            schedulers.reactorEvents())
        .subscribe(tick -> slotSink.onNext(this.startSlot.plus(tick)));
  }

  /** Stream fires current slot number at slot time start */
  @Override
  public Publisher<SlotNumber> getTickerStream() {
    return slotStream;
  }
}

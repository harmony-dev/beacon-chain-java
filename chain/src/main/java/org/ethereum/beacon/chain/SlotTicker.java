package org.ethereum.beacon.chain;

import java.time.Duration;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
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
  private final SpecHelpers spec;
  private final BeaconState state;

  private final Schedulers schedulers;
  private final SimpleProcessor<SlotNumber> slotStream;

  private SlotNumber startSlot;

  private Scheduler scheduler;

  public SlotTicker(SpecHelpers spec, BeaconState state, Schedulers schedulers) {
    this.spec = spec;
    this.state = state;
    this.schedulers = schedulers;

    slotStream = new SimpleProcessor<>(this.schedulers.reactorEvents(), "SlotTicker.slot");
  }

  /** Execute to start {@link SlotNumber} propagation */
  @Override
  public void start() {
    this.scheduler = schedulers.newSingleThreadDaemon("slot-ticker");

    SlotNumber nextSlot = spec.get_current_slot(state, schedulers.getCurrentTime()).increment();
    Time period = spec.getConstants().getSecondsPerSlot();
    startImpl(nextSlot, period, scheduler);
  }

  @Override
  public void stop() {}

  private void startImpl(SlotNumber startSlot, Time period, Scheduler scheduler) {
    this.startSlot = startSlot;

    Time startSlotTime = spec.get_slot_start_time(state, startSlot);
    long delayMillis =
        Math.max(0, startSlotTime.getMillis().getValue() - schedulers.getCurrentTime());
    Flux.interval(
            Duration.ofMillis(delayMillis),
            Duration.ofSeconds(period.getValue()),
            schedulers.reactorEvents())
        .subscribe(tick -> slotStream.onNext(this.startSlot.plus(tick)));
  }

  /** Stream fires current slot number at slot time start */
  @Override
  public Publisher<SlotNumber> getTickerStream() {
    return slotStream;
  }
}

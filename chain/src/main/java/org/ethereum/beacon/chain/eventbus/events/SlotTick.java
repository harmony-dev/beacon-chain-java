package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.core.types.SlotNumber;

public class SlotTick implements Event<SlotNumber> {

  public static SlotTick wrap(SlotNumber slot) {
    return new SlotTick(slot);
  }

  private final SlotNumber slot;

  public SlotTick(SlotNumber slot) {
    this.slot = slot;
  }

  @Override
  public SlotNumber getValue() {
    return slot;
  }
}

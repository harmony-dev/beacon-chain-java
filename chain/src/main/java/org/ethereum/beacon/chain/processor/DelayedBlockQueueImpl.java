package org.ethereum.beacon.chain.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.BlockUnparked;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;

public class DelayedBlockQueueImpl implements DelayedBlockQueue {

  private final EventBus eventBus;

  private final TreeMap<SlotNumber, List<BeaconBlock>> blocks = new TreeMap<>();

  public DelayedBlockQueueImpl(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void onTick(SlotNumber slot) {
    SortedMap<SlotNumber, List<BeaconBlock>> pastBlocks = blocks.headMap(slot, true);
    pastBlocks
        .values()
        .forEach(slotBucket -> slotBucket.forEach(b -> eventBus.publish(BlockUnparked.wrap(b))));
    blocks.keySet().removeAll(pastBlocks.keySet());
  }

  @Override
  public void onBlock(BeaconBlock block) {
    List<BeaconBlock> slotBucket =
        blocks.computeIfAbsent(block.getSlot(), slot -> new ArrayList<>());
    slotBucket.add(block);
  }
}

package org.ethereum.beacon.chain.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;

public class DelayedBlockQueueImpl implements DelayedBlockQueue {

  private final TreeMap<SlotNumber, List<BeaconBlock>> blocks = new TreeMap<>();

  private Consumer<BeaconBlock> subscriber;

  @Override
  public void onTick(SlotNumber slot) {
    SortedMap<SlotNumber, List<BeaconBlock>> pastBlocks = blocks.headMap(slot, true);
    if (subscriber != null) {
      pastBlocks.values().forEach(slotBucket -> slotBucket.forEach(subscriber));
    }
    blocks.keySet().removeAll(pastBlocks.keySet());
  }

  @Override
  public void onBlock(BeaconBlock block) {
    List<BeaconBlock> slotBucket =
        blocks.computeIfAbsent(block.getSlot(), slot -> new ArrayList<>());
    slotBucket.add(block);
  }

  @Override
  public void subscribe(Consumer<BeaconBlock> subscriber) {
    this.subscriber = subscriber;
  }
}

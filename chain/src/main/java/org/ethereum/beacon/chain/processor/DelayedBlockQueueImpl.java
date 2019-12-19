package org.ethereum.beacon.chain.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.BlockBatchDequeued;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;

public class DelayedBlockQueueImpl implements DelayedBlockQueue {

  private final EventBus eventBus;

  private final TreeMap<SlotNumber, Set<SignedBeaconBlock>> blocks = new TreeMap<>();

  public DelayedBlockQueueImpl(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public void onTick(SlotNumber slot) {
    SortedMap<SlotNumber, Set<SignedBeaconBlock>> pastBlocks = blocks.headMap(slot, true);
    Set<SignedBeaconBlock> blocksDequeued =
        pastBlocks.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    eventBus.publish(BlockBatchDequeued.wrap(blocksDequeued));
    blocks.keySet().removeAll(pastBlocks.keySet());
  }

  @Override
  public void onBlock(SignedBeaconBlock signedBlock) {
    Set<SignedBeaconBlock> slotBucket =
        blocks.computeIfAbsent(signedBlock.getMessage().getSlot(), slot -> new HashSet<>());
    slotBucket.add(signedBlock);
  }
}

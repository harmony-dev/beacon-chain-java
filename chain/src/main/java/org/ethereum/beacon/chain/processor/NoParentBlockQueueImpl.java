package org.ethereum.beacon.chain.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.BlockBatchDequeued;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class NoParentBlockQueueImpl implements NoParentBlockQueue {

  private final EventBus eventBus;
  private final BeaconChainSpec spec;

  private final Map<Hash32, Set<BeaconBlock>> blocks = new HashMap<>();

  public NoParentBlockQueueImpl(EventBus eventBus, BeaconChainSpec spec) {
    this.eventBus = eventBus;
    this.spec = spec;
  }

  @Override
  public void onBlockWithNoParent(BeaconBlock block) {
    Set<BeaconBlock> bucket =
        blocks.computeIfAbsent(block.getParentRoot(), parentRoot -> new HashSet<>());
    bucket.add(block);
  }

  @Override
  public void onImportedBlock(BeaconBlock parent) {
    Set<BeaconBlock> bucket = blocks.remove(spec.signing_root(parent));
    if (bucket != null) {
      eventBus.publish(BlockBatchDequeued.wrap(bucket));
    }
  }
}

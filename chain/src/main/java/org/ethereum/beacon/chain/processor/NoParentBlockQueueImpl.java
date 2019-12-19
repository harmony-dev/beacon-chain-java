package org.ethereum.beacon.chain.processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.ethereum.beacon.chain.eventbus.EventBus;
import org.ethereum.beacon.chain.eventbus.events.BlockBatchDequeued;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class NoParentBlockQueueImpl implements NoParentBlockQueue {

  private final EventBus eventBus;
  private final BeaconChainSpec spec;

  private final Map<Hash32, Set<SignedBeaconBlock>> blocks = new HashMap<>();

  public NoParentBlockQueueImpl(EventBus eventBus, BeaconChainSpec spec) {
    this.eventBus = eventBus;
    this.spec = spec;
  }

  @Override
  public void onBlockWithNoParent(SignedBeaconBlock signedBlock) {
    Set<SignedBeaconBlock> bucket =
        blocks.computeIfAbsent(
            signedBlock.getMessage().getParentRoot(), parentRoot -> new HashSet<>());
    bucket.add(signedBlock);
  }

  @Override
  public void onImportedBlock(SignedBeaconBlock signedParent) {
    Set<SignedBeaconBlock> bucket = blocks.remove(spec.hash_tree_root(signedParent.getMessage()));
    if (bucket != null) {
      eventBus.publish(BlockBatchDequeued.wrap(bucket));
    }
  }
}

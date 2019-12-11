package org.ethereum.beacon.chain.eventbus.events;

import java.util.Collection;
import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.core.BeaconBlock;

public class BlockBatchDequeued implements Event<Collection<BeaconBlock>> {

  public static BlockBatchDequeued wrap(Collection<BeaconBlock> blocks) {
    return new BlockBatchDequeued(blocks);
  }

  private final Collection<BeaconBlock> blocks;

  public BlockBatchDequeued(Collection<BeaconBlock> blocks) {
    this.blocks = blocks;
  }

  @Override
  public Collection<BeaconBlock> getValue() {
    return blocks;
  }
}

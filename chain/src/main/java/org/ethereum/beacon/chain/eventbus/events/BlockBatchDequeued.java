package org.ethereum.beacon.chain.eventbus.events;

import java.util.Collection;
import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public class BlockBatchDequeued implements Event<Collection<SignedBeaconBlock>> {

  public static BlockBatchDequeued wrap(Collection<SignedBeaconBlock> blocks) {
    return new BlockBatchDequeued(blocks);
  }

  private final Collection<SignedBeaconBlock> blocks;

  public BlockBatchDequeued(Collection<SignedBeaconBlock> blocks) {
    this.blocks = blocks;
  }

  @Override
  public Collection<SignedBeaconBlock> getValue() {
    return blocks;
  }
}

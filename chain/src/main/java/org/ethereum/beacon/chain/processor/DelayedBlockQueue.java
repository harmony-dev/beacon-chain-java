package org.ethereum.beacon.chain.processor;

import java.util.function.Consumer;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;

public interface DelayedBlockQueue {

  void onTick(SlotNumber slot);

  void onBlock(BeaconBlock block);

  void subscribe(Consumer<BeaconBlock> subscriber);
}

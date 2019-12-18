package org.ethereum.beacon.chain.processor;

import org.ethereum.beacon.core.BeaconBlock;

public interface NoParentBlockQueue {

  void onBlockWithNoParent(BeaconBlock block);

  void onImportedBlock(BeaconBlock parent);
}

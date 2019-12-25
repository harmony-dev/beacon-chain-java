package org.ethereum.beacon.chain.processor;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

public interface NoParentBlockQueue {

  void onBlockWithNoParent(SignedBeaconBlock signedBlock);

  void onImportedBlock(SignedBeaconBlock signedParent);
}

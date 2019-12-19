package org.ethereum.beacon.chain.processor;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;

public interface DelayedBlockQueue {

  void onTick(SlotNumber slot);

  void onBlock(SignedBeaconBlock signedBlock);
}

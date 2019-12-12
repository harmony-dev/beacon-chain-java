package org.ethereum.beacon.chain.processor;

import java.util.Collections;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperationsState;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlock;

public class BeaconStateAtTheTip extends ObservableBeaconState {
  public BeaconStateAtTheTip(BeaconBlock head, BeaconStateEx latestSlotState) {
    super(head, latestSlotState, new PendingOperationsState(Collections.emptyList()));
  }
}

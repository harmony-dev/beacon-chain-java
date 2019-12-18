package org.ethereum.beacon.chain.processor;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;

public interface AttestationPool {

  void onTick(SlotNumber slot);

  void onAttestation(Attestation attestation);

  void onStateAtTheBeginningOfSlot(ObservableBeaconState stateAtTheTip);
}

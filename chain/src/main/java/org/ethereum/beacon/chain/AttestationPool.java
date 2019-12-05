package org.ethereum.beacon.chain;

import java.util.List;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;

public interface AttestationPool {

  void onTick(SlotNumber slot);

  void onAttestation(Attestation attestation);

  List<Attestation> getOffChainAttestations(BeaconState state);
}

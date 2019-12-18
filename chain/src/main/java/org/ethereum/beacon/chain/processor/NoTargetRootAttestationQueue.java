package org.ethereum.beacon.chain.processor;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;

public interface NoTargetRootAttestationQueue {

  void onBlock(BeaconBlock block);

  void onAttestation(Attestation attestation);
}

package org.ethereum.beacon.chain.processor;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;

public interface NoBlockRootAttestationQueue {

  void onBlock(SignedBeaconBlock signedBlock);

  void onAttestation(Attestation attestation);
}

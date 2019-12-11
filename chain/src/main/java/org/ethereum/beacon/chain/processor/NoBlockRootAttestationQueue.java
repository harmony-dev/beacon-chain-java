package org.ethereum.beacon.chain.processor;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;

public interface NoBlockRootAttestationQueue {

  void onBlock(BeaconBlock block);

  void onAttestation(Attestation attestation);
}

package org.ethereum.beacon;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.reactivestreams.Publisher;

public interface WireApi {

  void sendProposedBlock(BeaconBlock block);

  void sendAttestation(Attestation attestation);

  Publisher<BeaconBlock> inboundBlocksStream();

  Publisher<Attestation> inboundAttestationsStream();
}

package org.ethereum.beacon.wire;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.reactivestreams.Publisher;

public interface WireApiSub {

  void sendProposedBlock(BeaconBlock block);

  void sendAttestation(Attestation attestation);

  Publisher<BeaconBlock> inboundBlocksStream();

  Publisher<Attestation> inboundAttestationsStream();
}

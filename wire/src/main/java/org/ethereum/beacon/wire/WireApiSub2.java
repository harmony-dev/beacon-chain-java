package org.ethereum.beacon.wire;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.reactivestreams.Publisher;

public interface WireApiSub2 {

  void newBlock(BeaconBlock block);

  void newAttestation(Attestation attestation);

}

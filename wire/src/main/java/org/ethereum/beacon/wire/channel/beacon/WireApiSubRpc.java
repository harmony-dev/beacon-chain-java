package org.ethereum.beacon.wire.channel.beacon;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.reactivestreams.Publisher;

public interface WireApiSubRpc {

  void newBlock(BeaconBlock block);

  void newAttestation(Attestation attestation);

}

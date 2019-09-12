package org.ethereum.beacon.wire.impl.plain.channel.beacon;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;

public interface WireApiSubRpc {

  void newBlock(BeaconBlock block);

  void newAttestation(Attestation attestation);

}

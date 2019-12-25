package org.ethereum.beacon.wire.impl.plain.channel.beacon;

import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;

public interface WireApiSubRpc {

  void newBlock(SignedBeaconBlock block);

  void newAttestation(Attestation attestation);
}

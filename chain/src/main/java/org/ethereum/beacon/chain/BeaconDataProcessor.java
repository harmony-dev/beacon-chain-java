package org.ethereum.beacon.chain;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.Time;

public interface BeaconDataProcessor {

  void onTick(Time time);

  void onBlock(BeaconBlock block);

  void onAttestation(Attestation attestation);
}

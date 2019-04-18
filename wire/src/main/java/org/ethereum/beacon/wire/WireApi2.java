package org.ethereum.beacon.wire;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;

public interface WireApi2 {

  CompletableFuture<Void> newBlock(BeaconBlock block);

  CompletableFuture<Void> newAttestation(Attestation attestation);

}

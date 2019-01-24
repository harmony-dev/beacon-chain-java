package org.ethereum.beacon.chain.observer;

import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;

/** A pending state interface. */
public interface PendingOperations {

  List<Attestation> getAttestations();
}

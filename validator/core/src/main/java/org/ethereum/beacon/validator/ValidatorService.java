package org.ethereum.beacon.validator;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.reactivestreams.Publisher;

/** Interface of validator service. */
public interface ValidatorService {

  Publisher<SignedBeaconBlock> getProposedBlocksStream();

  Publisher<Attestation> getAttestationsStream();

  /** Starts the service. */
  void start();

  /** Stops the service. */
  void stop();
}

package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.operations.Attestation;

public class AttestationMetNoTargetRoot extends AbstractAttestationEvent {

  public static AttestationMetNoTargetRoot wrap(Attestation attestation) {
    return new AttestationMetNoTargetRoot(attestation);
  }

  public AttestationMetNoTargetRoot(Attestation attestation) {
    super(attestation);
  }
}

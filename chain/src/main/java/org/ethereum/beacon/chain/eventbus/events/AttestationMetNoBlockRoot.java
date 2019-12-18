package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.operations.Attestation;

public class AttestationMetNoBlockRoot extends AbstractAttestationEvent {

  public static AttestationMetNoBlockRoot wrap(Attestation attestation) {
    return new AttestationMetNoBlockRoot(attestation);
  }

  public AttestationMetNoBlockRoot(Attestation attestation) {
    super(attestation);
  }
}

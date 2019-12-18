package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.operations.Attestation;

public class AttestationProduced extends AbstractAttestationEvent {

  public static AttestationProduced wrap(Attestation attestation) {
    return new AttestationProduced(attestation);
  }

  public AttestationProduced(Attestation attestation) {
    super(attestation);
  }
}

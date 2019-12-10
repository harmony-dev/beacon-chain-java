package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.operations.Attestation;

public class AttestationConsiderationDelayed extends AbstractAttestationEvent {

  public static AttestationConsiderationDelayed wrap(Attestation attestation) {
    return new AttestationConsiderationDelayed(attestation);
  }

  public AttestationConsiderationDelayed(Attestation attestation) {
    super(attestation);
  }
}

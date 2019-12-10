package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.operations.Attestation;

public class AttestationUnparked extends AbstractAttestationEvent {

  public static AttestationUnparked wrap(Attestation attestation) {
    return new AttestationUnparked(attestation);
  }

  public AttestationUnparked(Attestation attestation) {
    super(attestation);
  }
}

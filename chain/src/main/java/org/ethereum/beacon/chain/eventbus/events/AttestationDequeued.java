package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.operations.Attestation;

public class AttestationDequeued extends AbstractAttestationEvent {

  public static AttestationDequeued wrap(Attestation attestation) {
    return new AttestationDequeued(attestation);
  }

  public AttestationDequeued(Attestation attestation) {
    super(attestation);
  }
}

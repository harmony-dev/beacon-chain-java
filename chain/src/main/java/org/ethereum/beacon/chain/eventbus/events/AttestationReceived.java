package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.operations.Attestation;

public class AttestationReceived extends AbstractAttestationEvent {

  public static AttestationReceived wrap(Attestation attestation) {
    return new AttestationReceived(attestation);
  }

  public AttestationReceived(Attestation attestation) {
    super(attestation);
  }
}

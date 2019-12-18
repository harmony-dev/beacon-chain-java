package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.core.operations.Attestation;

abstract class AbstractAttestationEvent implements Event<Attestation> {

  private final Attestation attestation;

  protected AbstractAttestationEvent(Attestation attestation) {
    this.attestation = attestation;
  }

  @Override
  public Attestation getValue() {
    return attestation;
  }
}

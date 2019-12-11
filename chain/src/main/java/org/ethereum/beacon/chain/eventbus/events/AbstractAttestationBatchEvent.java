package org.ethereum.beacon.chain.eventbus.events;

import java.util.Collection;
import org.ethereum.beacon.chain.eventbus.EventBus.Event;
import org.ethereum.beacon.core.operations.Attestation;

abstract class AbstractAttestationBatchEvent implements Event<Collection<Attestation>> {

  private final Collection<Attestation> attestations;

  protected AbstractAttestationBatchEvent(Collection<Attestation> attestations) {
    this.attestations = attestations;
  }

  @Override
  public Collection<Attestation> getValue() {
    return attestations;
  }
}

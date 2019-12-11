package org.ethereum.beacon.chain.eventbus.events;

import java.util.Collection;
import org.ethereum.beacon.core.operations.Attestation;

public class AttestationBatchDequeued extends AbstractAttestationBatchEvent {

  public static AttestationBatchDequeued wrap(Collection<Attestation> attestations) {
    return new AttestationBatchDequeued(attestations);
  }

  public AttestationBatchDequeued(Collection<Attestation> attestations) {
    super(attestations);
  }
}

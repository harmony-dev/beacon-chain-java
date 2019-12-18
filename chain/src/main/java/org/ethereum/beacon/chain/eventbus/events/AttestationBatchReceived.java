package org.ethereum.beacon.chain.eventbus.events;

import java.util.Collection;
import org.ethereum.beacon.core.operations.Attestation;

public class AttestationBatchReceived extends AbstractAttestationBatchEvent {

  public static AttestationBatchReceived wrap(Collection<Attestation> attestations) {
    return new AttestationBatchReceived(attestations);
  }

  public AttestationBatchReceived(Collection<Attestation> attestations) {
    super(attestations);
  }
}

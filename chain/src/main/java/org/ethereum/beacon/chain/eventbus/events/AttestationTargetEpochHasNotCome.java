package org.ethereum.beacon.chain.eventbus.events;

import org.ethereum.beacon.core.operations.Attestation;

public class AttestationTargetEpochHasNotCome extends AbstractAttestationEvent {

  public static AttestationTargetEpochHasNotCome wrap(Attestation attestation) {
    return new AttestationTargetEpochHasNotCome(attestation);
  }

  public AttestationTargetEpochHasNotCome(Attestation attestation) {
    super(attestation);
  }
}

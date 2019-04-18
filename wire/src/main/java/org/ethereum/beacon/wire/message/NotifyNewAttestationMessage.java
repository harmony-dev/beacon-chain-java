package org.ethereum.beacon.wire.message;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class NotifyNewAttestationMessage extends MessagePayload {

  @SSZ private final Attestation attestation;

  public NotifyNewAttestationMessage(Attestation attestation) {
    this.attestation = attestation;
  }

  public Attestation getAttestation() {
    return attestation;
  }
}

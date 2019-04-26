package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.MessagePayload;

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

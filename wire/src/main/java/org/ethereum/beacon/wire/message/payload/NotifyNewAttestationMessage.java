package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.MessagePayload;
import org.ethereum.beacon.wire.message.RequestMessagePayload;

@SSZSerializable
public class NotifyNewAttestationMessage extends RequestMessagePayload {
  public static final int METHOD_ID = 0xF02;

  @SSZ private final Attestation attestation;

  public NotifyNewAttestationMessage(Attestation attestation) {
    this.attestation = attestation;
  }

  public Attestation getAttestation() {
    return attestation;
  }

  @Override
  public int getMethodId() {
    return METHOD_ID;
  }

  @Override
  public String toString() {
    return "NotifyNewAttestationMessage{" +
        "attestation=" + attestation +
        '}';
  }
}

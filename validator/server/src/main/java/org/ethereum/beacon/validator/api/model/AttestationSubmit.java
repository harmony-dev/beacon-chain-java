package org.ethereum.beacon.validator.api.model;

import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.validator.api.convert.BeaconBlockConverter;

public class AttestationSubmit {
  private BlockData.BlockBodyData.IndexedAttestationData attestation;

  public AttestationSubmit() {}

  public AttestationSubmit(BlockData.BlockBodyData.IndexedAttestationData attestation) {
    this.attestation = attestation;
  }

  public static AttestationSubmit fromAttestation(IndexedAttestation attestation) {
    return new AttestationSubmit(BeaconBlockConverter.presentIndexedAttestation(attestation));
  }

  public IndexedAttestation createAttestation() {
    return BeaconBlockConverter.parseIndexedAttestation(getAttestation());
  }

  public BlockData.BlockBodyData.IndexedAttestationData getAttestation() {
    return attestation;
  }

  public void setAttestation(BlockData.BlockBodyData.IndexedAttestationData attestation) {
    this.attestation = attestation;
  }
}

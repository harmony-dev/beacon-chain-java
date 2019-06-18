package org.ethereum.beacon.validator.api.model;

public class AttestationSubmit {
  private BlockData.BlockBodyData.IndexedAttestationData attestation;

  public AttestationSubmit() {
  }

  public AttestationSubmit(BlockData.BlockBodyData.IndexedAttestationData attestation) {
    this.attestation = attestation;
  }

  public BlockData.BlockBodyData.IndexedAttestationData getAttestation() {
    return attestation;
  }

  public void setAttestation(BlockData.BlockBodyData.IndexedAttestationData attestation) {
    this.attestation = attestation;
  }
}

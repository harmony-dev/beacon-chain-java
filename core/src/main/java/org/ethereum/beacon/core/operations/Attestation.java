package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.util.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;

@SSZSerializable
public class Attestation {
  private final AttestationData data;
  private final BytesValue participationBitfield;
  private final BytesValue custodyBitfield;
  private final Bytes96 aggregatedSignature;

  public Attestation(
      AttestationData data,
      BytesValue participationBitfield,
      BytesValue custodyBitfield,
      Bytes96 aggregatedSignature) {
    this.data = data;
    this.participationBitfield = participationBitfield;
    this.custodyBitfield = custodyBitfield;
    this.aggregatedSignature = aggregatedSignature;
  }

  public AttestationData getData() {
    return data;
  }

  public BytesValue getParticipationBitfield() {
    return participationBitfield;
  }

  public BytesValue getCustodyBitfield() {
    return custodyBitfield;
  }

  public Bytes96 getAggregatedSignature() {
    return aggregatedSignature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Attestation that = (Attestation) o;
    return data.equals(that.data) &&
        participationBitfield.equals(that.participationBitfield) &&
        custodyBitfield.equals(that.custodyBitfield) &&
        aggregatedSignature.equals(that.aggregatedSignature);
  }
}

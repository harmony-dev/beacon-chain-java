package org.ethereum.beacon.core.operations;

import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;

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
}

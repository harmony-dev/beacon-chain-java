package org.ethereum.beacon.chain.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import tech.pegasys.artemis.util.collections.Bitlist;

public class AttestationAggregate {

  public static AttestationAggregate create(Attestation attestation) {
    List<BLSSignature> signatures = new ArrayList<>();
    signatures.add(attestation.getSignature());
    return new AttestationAggregate(
        attestation.getAggregationBits(),
        attestation.getCustodyBits(),
        attestation.getData(),
        signatures);
  }

  private final Bitlist aggregationBits;
  private final AttestationData data;
  private final Bitlist custodyBits;
  private final List<BLSSignature> signatures;

  public AttestationAggregate(
      Bitlist aggregationBits,
      Bitlist custodyBits,
      AttestationData data,
      List<BLSSignature> signatures) {
    this.aggregationBits = aggregationBits;
    this.custodyBits = custodyBits;
    this.data = data;
    this.signatures = signatures;
  }

  public boolean add(Attestation attestation) {
    if (isAggregatable(attestation)) {
      aggregationBits.or(attestation.getAggregationBits());
      custodyBits.or(attestation.getCustodyBits());
      signatures.add(attestation.getSignature());

      return true;
    } else {
      return false;
    }
  }

  private boolean isAggregatable(Attestation attestation) {
    if (!data.equals(attestation.getData())) {
      return false;
    }

    if (!aggregationBits.and(attestation.getAggregationBits()).isEmpty()) {
      return false;
    }

    return true;
  }

  public Attestation getAggregate(SpecConstants specConstants) {
    BLSSignature signature =
        BLSSignature.wrap(
            BLS381.Signature.aggregate(
                    signatures.stream()
                        .map(BLS381.Signature::createWithoutValidation)
                        .collect(Collectors.toList()))
                .getEncoded());

    return new Attestation(aggregationBits, data, custodyBits, signature, specConstants);
  }
}

package org.ethereum.beacon.validator.proposer;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381.Signature;
import tech.pegasys.artemis.util.collections.Bitlist;

public class AggregateOperator implements Function<List<Attestation>, Attestation> {

  private final BeaconChainSpec spec;

  public AggregateOperator(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public Attestation apply(List<Attestation> attestations) {
    assert attestations.size() > 0;

    if (attestations.size() == 1) {
      return attestations.get(0);
    }

    Bitlist aggregationBits =
        attestations.stream().map(Attestation::getAggregationBits).reduce(Bitlist::or).get();
    AttestationData data = attestations.get(0).getData();
    BLSSignature signature =
        BLSSignature.wrap(
            Signature.aggregate(
                    attestations.stream()
                        .map(Attestation::getSignature)
                        .map(Signature::createWithoutValidation)
                        .collect(Collectors.toList()))
                .getEncoded());

    return new Attestation(aggregationBits, data, signature, spec.getConstants());
  }
}

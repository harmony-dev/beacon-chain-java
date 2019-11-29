package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.test.type.DataMapperAccessor;
import org.ethereum.beacon.test.type.model.BeaconStateData;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;

import static org.ethereum.beacon.test.StateTestUtils.parseAttestationData;

public interface AttestationField extends DataMapperAccessor {
  default Attestation getAttestation(SpecConstants constants) {
    final String key = useSszWhenPossible() ? "attestation.ssz" : "attestation.yaml";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`attestation` not defined");
    }

    // SSZ
    if (useSszWhenPossible()) {
      return getSszSerializer().decode(getFiles().get(key), Attestation.class);
    }

    // YAML
    try {
      BeaconStateData.AttestationData attestationData =
          getMapper().readValue(getFiles().get(key).extractArray(), BeaconStateData.AttestationData.class);
      BytesValue aggValue = BytesValue.fromHexString(attestationData.getAggregationBits());

      Attestation attestation =
          new Attestation(
              Bitlist.of(aggValue, constants.getMaxValidatorsPerCommittee().getValue()),
              parseAttestationData((attestationData.getData())),
              BLSSignature.wrap(Bytes96.fromHexString(attestationData.getSignature())),
              constants);

      return attestation;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

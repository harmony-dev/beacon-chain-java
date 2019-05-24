package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.Attestation;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class AttestationSerializer implements ObjectSerializer<Attestation> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private AttestationDataSerializer attestationDataSerializer;

  public AttestationSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.attestationDataSerializer = new AttestationDataSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return Attestation.class;
  }

  @Override
  public ObjectNode map(Attestation instance) {
    ObjectNode attestation = mapper.createObjectNode();
    attestation.put("aggregation_bitfield", instance.getAggregationBitfield().toString());
    attestation.set("data", attestationDataSerializer.map(instance.getData()));
    attestation.put("custody_bitfield", instance.getCustodyBitfield().toString());
    attestation.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return attestation;
  }
}

package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class IndexedAttestationSerializer implements ObjectSerializer<IndexedAttestation> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private AttestationDataSerializer attestationDataSerializer;

  public IndexedAttestationSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.attestationDataSerializer = new AttestationDataSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return IndexedAttestation.class;
  }

  @Override
  public ObjectNode map(IndexedAttestation instance) {
    ObjectNode slashableAttestation = mapper.createObjectNode();
    ArrayNode indices0 = mapper.createArrayNode();
    instance.getCustodyBit0Indices().stream().map(ComparableBigIntegerNode::valueOf).forEachOrdered(indices0::add);
    slashableAttestation.set("custody_bit_0_indices", indices0);
    ArrayNode indices1 = mapper.createArrayNode();
    instance.getCustodyBit1Indices().stream().map(ComparableBigIntegerNode::valueOf).forEachOrdered(indices1::add);
    slashableAttestation.set("custody_bit_1_indices", indices1);
    slashableAttestation.set("data", attestationDataSerializer.map(instance.getData()));
    slashableAttestation.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return slashableAttestation;
  }
}

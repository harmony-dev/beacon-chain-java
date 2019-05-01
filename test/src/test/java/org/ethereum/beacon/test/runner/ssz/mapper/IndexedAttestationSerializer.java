package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.slashing.SlashableAttestation;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;
// TODO: should be IndexedAttestation
public class IndexedAttestationSerializer implements ObjectSerializer<SlashableAttestation> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private AttestationDataSerializer attestationDataSerializer;

  public IndexedAttestationSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.attestationDataSerializer = new AttestationDataSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return SlashableAttestation.class;
  }

  @Override
  public ObjectNode map(SlashableAttestation instance) {
    ObjectNode slashableAttestation = mapper.createObjectNode();
    ArrayNode indices0 = mapper.createArrayNode();
    instance.getValidatorIndices().stream().map(ObjectSerializer::convert).forEachOrdered(indices0::add);
    slashableAttestation.set("custody_bit_0_indices", indices0);
//    slashableAttestation.put("custody_bit_1_indices", instance.get); TODO
    slashableAttestation.set("data", attestationDataSerializer.map(instance.getData()));
    slashableAttestation.put("signature", BytesValue.wrap(instance.getAggregateSingature().getArrayUnsafe()).toString());
    return slashableAttestation;
  }
}

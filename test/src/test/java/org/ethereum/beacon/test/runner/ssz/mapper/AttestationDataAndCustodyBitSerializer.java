package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;

public class AttestationDataAndCustodyBitSerializer implements ObjectSerializer<AttestationDataAndCustodyBit> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private AttestationDataSerializer attestationDataSerializer;

  public AttestationDataAndCustodyBitSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.attestationDataSerializer = new AttestationDataSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return AttestationDataAndCustodyBit.class;
  }

  @Override
  public ObjectNode map(AttestationDataAndCustodyBit instance) {
    ObjectNode attestationDataAndCustodyBit = mapper.createObjectNode();
    attestationDataAndCustodyBit.set("data", attestationDataSerializer.map(instance.getData()));
    attestationDataAndCustodyBit.put("custody_bit", instance.isCustodyBit());
    return attestationDataAndCustodyBit;
  }
}

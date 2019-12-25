package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.attestation.AttestationData;

public class AttestationDataSerializer implements ObjectSerializer<AttestationData> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private CheckpointSerializer checkpointSerializer;

  public AttestationDataSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.checkpointSerializer = new CheckpointSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return AttestationData.class;
  }

  @Override
  public ObjectNode map(AttestationData instance) {
    ObjectNode attestationData = mapper.createObjectNode();
    attestationData.set("slot", ComparableBigIntegerNode.valueOf(instance.getSlot()));
    attestationData.set("index", ComparableBigIntegerNode.valueOf(instance.getIndex()));
    attestationData.put("beacon_block_root", instance.getBeaconBlockRoot().toString());
    attestationData.set("source", checkpointSerializer.map(instance.getSource()));
    attestationData.set("target", checkpointSerializer.map(instance.getTarget()));
    return attestationData;
  }
}

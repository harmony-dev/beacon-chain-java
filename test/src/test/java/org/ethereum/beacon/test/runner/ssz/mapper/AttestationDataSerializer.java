package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.attestation.AttestationData;

public class AttestationDataSerializer implements ObjectSerializer<AttestationData> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public AttestationDataSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return AttestationData.class;
  }

  @Override
  public ObjectNode map(AttestationData instance) {
    ObjectNode attestationData = mapper.createObjectNode();
    attestationData.put("beacon_block_root", instance.getBeaconBlockRoot().toString());
    ObjectSerializer.setUint64Field(attestationData, "source_epoch", instance.getSourceEpoch());
    attestationData.put("source_root", instance.getSourceRoot().toString());
    ObjectSerializer.setUint64Field(attestationData, "target_epoch", instance.getTargetEpoch());
    attestationData.put("target_root", instance.getTargetRoot().toString());
    ObjectSerializer.setUint64Field(attestationData, "shard", instance.getShard());
    attestationData.put("previous_crosslink_root", instance.getPreviousCrosslinkRoot().toString());
    attestationData.put("crosslink_data_root", instance.getCrosslinkDataRoot().toString());
    return attestationData;
  }
}

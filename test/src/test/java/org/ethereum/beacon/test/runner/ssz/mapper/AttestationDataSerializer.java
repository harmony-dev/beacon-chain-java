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
    attestationData.set("source_epoch", ComparableBigIntegerNode.valueOf(instance.getSource().getEpoch()));
    attestationData.put("source_root", instance.getSource().getRoot().toString());
    attestationData.set("target_epoch", ComparableBigIntegerNode.valueOf(instance.getTarget().getEpoch()));
    attestationData.put("target_root", instance.getTarget().getRoot().toString());
    attestationData.set("slot", ComparableBigIntegerNode.valueOf(instance.getSlot()));
    attestationData.set("index", ComparableBigIntegerNode.valueOf(instance.getIndex()));
    return attestationData;
  }
}

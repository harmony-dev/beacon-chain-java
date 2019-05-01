package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class VoluntaryExitSerializer implements ObjectSerializer<VoluntaryExit> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public VoluntaryExitSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return VoluntaryExit.class;
  }

  @Override
  public ObjectNode map(VoluntaryExit instance) {
    ObjectNode voluntaryExit = mapper.createObjectNode();
    ObjectSerializer.setUint64Field(voluntaryExit, "epoch", instance.getEpoch());
    ObjectSerializer.setUint64Field(voluntaryExit, "validator_index", instance.getValidatorIndex());
    voluntaryExit.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return voluntaryExit;
  }
}

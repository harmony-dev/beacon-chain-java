package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.envelops.SignedVoluntaryExit;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class VoluntaryExitSerializer implements ObjectSerializer<SignedVoluntaryExit> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public VoluntaryExitSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return VoluntaryExit.class;
  }

  @Override
  public ObjectNode map(SignedVoluntaryExit instance) {
    ObjectNode voluntaryExit = mapper.createObjectNode();
    voluntaryExit.set("epoch", ComparableBigIntegerNode.valueOf(instance.getMessage().getEpoch()));
    voluntaryExit.set("validator_index", ComparableBigIntegerNode.valueOf(instance.getMessage().getValidatorIndex()));
    voluntaryExit.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return voluntaryExit;
  }
}

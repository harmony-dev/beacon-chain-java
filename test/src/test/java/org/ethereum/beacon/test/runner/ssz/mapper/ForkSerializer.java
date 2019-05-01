package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.state.Fork;

public class ForkSerializer implements ObjectSerializer<Fork> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public ForkSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return Fork.class;
  }

  @Override
  public ObjectNode map(Fork instance) {
    ObjectNode fork = mapper.createObjectNode();
    fork.put("previous_version", instance.getPreviousVersion().toString());
    fork.put("current_version", instance.getCurrentVersion().toString());
    ObjectSerializer.setUint64Field(fork, "epoch", instance.getEpoch());
    return fork;
  }
}

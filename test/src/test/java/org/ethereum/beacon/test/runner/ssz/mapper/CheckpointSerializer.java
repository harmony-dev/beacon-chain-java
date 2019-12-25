package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.state.Checkpoint;

public class CheckpointSerializer implements ObjectSerializer<Checkpoint> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public CheckpointSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return Checkpoint.class;
  }

  @Override
  public ObjectNode map(Checkpoint instance) {
    ObjectNode checkpoint = mapper.createObjectNode();
    checkpoint.put("epoch", ComparableBigIntegerNode.valueOf(instance.getEpoch()));
    checkpoint.put("root", instance.getRoot().toString());
    return checkpoint;
  }
}

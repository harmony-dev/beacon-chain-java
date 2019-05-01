package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.state.HistoricalBatch;

import java.util.Objects;

public class HistoricalBatchSerializer implements ObjectSerializer<HistoricalBatch> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public HistoricalBatchSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return HistoricalBatch.class;
  }

  @Override
  public ObjectNode map(HistoricalBatch instance) {
    ObjectNode historicalBatch = mapper.createObjectNode();
    ArrayNode blockRootsNode = mapper.createArrayNode();
    instance.getBlockRoots().stream().map(Objects::toString).forEachOrdered(blockRootsNode::add);
    historicalBatch.set("block_roots", blockRootsNode);
    ArrayNode stateRootsNode = mapper.createArrayNode();
    instance.getStateRoots().stream().map(Objects::toString).forEachOrdered(stateRootsNode::add);
    historicalBatch.set("state_roots", stateRootsNode);
    return historicalBatch;
  }
}

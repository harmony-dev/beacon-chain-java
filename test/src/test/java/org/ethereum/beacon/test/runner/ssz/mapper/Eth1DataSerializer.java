package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.state.Eth1Data;

public class Eth1DataSerializer implements ObjectSerializer<Eth1Data> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public Eth1DataSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return Eth1Data.class;
  }

  @Override
  public ObjectNode map(Eth1Data instance) {
    ObjectNode eth1Data = mapper.createObjectNode();
    eth1Data.put("deposit_root", instance.getDepositRoot().toString());
    ObjectSerializer.setUint64Field(eth1Data, "deposit_count", instance.getDepositCount());
    eth1Data.put("block_hash", instance.getBlockHash().toString());
    return eth1Data;
  }
}

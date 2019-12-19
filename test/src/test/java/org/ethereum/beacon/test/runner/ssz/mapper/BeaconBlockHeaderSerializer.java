package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.BeaconBlockHeader;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class BeaconBlockHeaderSerializer implements ObjectSerializer<BeaconBlockHeader> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public BeaconBlockHeaderSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return BeaconBlockHeader.class;
  }

  @Override
  public ObjectNode map(BeaconBlockHeader instance) {
    ObjectNode beaconBlockHeader = mapper.createObjectNode();
    beaconBlockHeader.set("slot", ComparableBigIntegerNode.valueOf(instance.getSlot()));
    beaconBlockHeader.put("previous_block_root", instance.getParentRoot().toString());
    beaconBlockHeader.put("state_root", instance.getStateRoot().toString());
    beaconBlockHeader.put("block_body_root", instance.getBodyRoot().toString());
    return beaconBlockHeader;
  }
}

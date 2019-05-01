package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class BlockSerializer implements ObjectSerializer<BeaconBlock> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private BlockBodySerializer blockBodySerializer;

  public BlockSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.blockBodySerializer = new BlockBodySerializer(mapper);
  }

  @Override
  public Class accepts() {
    return BeaconBlock.class;
  }

  @Override
  public ObjectNode map(BeaconBlock instance) {
    ObjectNode beaconBlock = mapper.createObjectNode();
    ObjectSerializer.setUint64Field(beaconBlock, "slot", instance.getSlot());
    beaconBlock.put("previous_block_root", instance.getPreviousBlockRoot().toString());
    beaconBlock.put("state_root", instance.getStateRoot().toString());
    beaconBlock.set("body", blockBodySerializer.map(instance.getBody()));
    beaconBlock.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return beaconBlock;
  }
}

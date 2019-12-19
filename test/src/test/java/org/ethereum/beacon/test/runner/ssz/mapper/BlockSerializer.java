package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class BlockSerializer implements ObjectSerializer<SignedBeaconBlock> {
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
  public ObjectNode map(SignedBeaconBlock instance) {
    ObjectNode beaconBlock = mapper.createObjectNode();
    beaconBlock.set("slot", ComparableBigIntegerNode.valueOf(instance.getMessage().getSlot()));
    beaconBlock.put("previous_block_root", instance.getMessage().getParentRoot().toString());
    beaconBlock.put("state_root", instance.getMessage().getStateRoot().toString());
    beaconBlock.set("body", blockBodySerializer.map(instance.getMessage().getBody()));
    beaconBlock.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return beaconBlock;
  }
}

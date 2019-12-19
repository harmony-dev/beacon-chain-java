package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.envelops.SignedBeaconBlockHeader;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SignedBeaconBlockHeaderSerializer implements ObjectSerializer<SignedBeaconBlockHeader> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public SignedBeaconBlockHeaderSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return BeaconBlockHeader.class;
  }

  @Override
  public ObjectNode map(SignedBeaconBlockHeader instance) {
    ObjectNode beaconBlockHeader = mapper.createObjectNode();
    beaconBlockHeader.set("slot", ComparableBigIntegerNode.valueOf(instance.getMessage().getSlot()));
    beaconBlockHeader.put("previous_block_root", instance.getMessage().getParentRoot().toString());
    beaconBlockHeader.put("state_root", instance.getMessage().getStateRoot().toString());
    beaconBlockHeader.put("block_body_root", instance.getMessage().getBodyRoot().toString());
    beaconBlockHeader.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return beaconBlockHeader;
  }
}

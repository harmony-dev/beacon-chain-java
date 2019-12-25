package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SignedBlockSerializer implements ObjectSerializer<SignedBeaconBlock> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private BlockSerializer blockSerializer;

  public SignedBlockSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.blockSerializer = new BlockSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return SignedBeaconBlock.class;
  }

  @Override
  public ObjectNode map(SignedBeaconBlock instance) {
    ObjectNode signedBeaconBlock = mapper.createObjectNode();
    signedBeaconBlock.set("message", blockSerializer.map(instance.getMessage()));
    signedBeaconBlock.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return signedBeaconBlock;
  }
}

package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.envelops.SignedBeaconBlockHeader;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SignedBeaconBlockHeaderSerializer implements ObjectSerializer<SignedBeaconBlockHeader> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private BeaconBlockHeaderSerializer beaconBlockHeaderSerializer;

  public SignedBeaconBlockHeaderSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.beaconBlockHeaderSerializer = new BeaconBlockHeaderSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return SignedBeaconBlockHeader.class;
  }

  @Override
  public ObjectNode map(SignedBeaconBlockHeader instance) {
    ObjectNode signedBeaconBlockHeader = mapper.createObjectNode();
    signedBeaconBlockHeader.set("message", beaconBlockHeaderSerializer.map(instance.getMessage()));
    signedBeaconBlockHeader.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return signedBeaconBlockHeader;
  }
}

package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.envelops.SignedVoluntaryExit;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SignedVoluntaryExitSerializer implements ObjectSerializer<SignedVoluntaryExit> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private VoluntaryExitSerializer voluntaryExitSerializer;

  public SignedVoluntaryExitSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.voluntaryExitSerializer = new VoluntaryExitSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return SignedVoluntaryExit.class;
  }

  @Override
  public ObjectNode map(SignedVoluntaryExit instance) {
    ObjectNode signedVoluntaryExit = mapper.createObjectNode();
    signedVoluntaryExit.set("message", voluntaryExitSerializer.map(instance.getMessage()));
    signedVoluntaryExit.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return signedVoluntaryExit;
  }
}

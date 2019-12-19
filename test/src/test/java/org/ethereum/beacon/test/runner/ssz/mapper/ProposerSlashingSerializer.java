package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.ProposerSlashing;

public class ProposerSlashingSerializer implements ObjectSerializer<ProposerSlashing> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private SignedBeaconBlockHeaderSerializer signedBeaconBlockHeaderSerializer;

  public ProposerSlashingSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.signedBeaconBlockHeaderSerializer = new SignedBeaconBlockHeaderSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return ProposerSlashing.class;
  }

  @Override
  public ObjectNode map(ProposerSlashing instance) {
    ObjectNode proposerSlashing = mapper.createObjectNode();
    proposerSlashing.put("proposer_index", instance.getProposerIndex().getValue());
    proposerSlashing.set("signed_header_1", signedBeaconBlockHeaderSerializer.map(instance.getSignedHeader1()));
    proposerSlashing.set("signed_header_2", signedBeaconBlockHeaderSerializer.map(instance.getSignedHeader2()));
    return proposerSlashing;
  }
}

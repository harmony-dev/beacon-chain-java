package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.state.HistoricalBatch;

import java.util.Objects;

public class ProposerSlashingSerializer implements ObjectSerializer<ProposerSlashing> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private BeaconBlockHeaderSerializer beaconBlockHeaderSerializer;

  public ProposerSlashingSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.beaconBlockHeaderSerializer = new BeaconBlockHeaderSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return ProposerSlashing.class;
  }

  @Override
  public ObjectNode map(ProposerSlashing instance) {
    ObjectNode proposerSlashing = mapper.createObjectNode();
    proposerSlashing.put("proposer_index", instance.getProposerIndex().getValue());
    proposerSlashing.set("header_1", beaconBlockHeaderSerializer.map(instance.getHeader1()));
    proposerSlashing.set("header_2", beaconBlockHeaderSerializer.map(instance.getHeader2()));
    return proposerSlashing;
  }
}

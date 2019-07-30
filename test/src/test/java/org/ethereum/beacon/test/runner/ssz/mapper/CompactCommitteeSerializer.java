package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.state.CompactCommittee;

import java.util.Objects;

public class CompactCommitteeSerializer implements ObjectSerializer<CompactCommittee> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public CompactCommitteeSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return CompactCommittee.class;
  }

  @Override
  public ObjectNode map(CompactCommittee instance) {
    ObjectNode compactCommittee = mapper.createObjectNode();
    ArrayNode compactValidators = mapper.createArrayNode();
    instance.getCompactValidators().stream().map(Objects::toString).forEachOrdered(compactValidators::add);
    compactCommittee.set("compact_validators", compactValidators);
    ArrayNode pubkeys = mapper.createArrayNode();
    instance.getPubkeys().stream().map(Objects::toString).forEachOrdered(pubkeys::add);
    compactCommittee.set("pubkeys", pubkeys);
    return compactCommittee;
  }
}

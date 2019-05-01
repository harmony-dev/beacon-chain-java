package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.Deposit;

import java.util.Objects;

public class DepositSerializer implements ObjectSerializer<Deposit> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private DepositDataSerializer depositDataSerializer;

  public DepositSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.depositDataSerializer = new DepositDataSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return Deposit.class;
  }

  @Override
  public ObjectNode map(Deposit instance) {
    ObjectNode attestation = mapper.createObjectNode();
    ArrayNode proofNode = mapper.createArrayNode();
    instance.getProof().stream().map(Objects::toString).forEachOrdered(proofNode::add);
    attestation.set("proof", proofNode);
    attestation.put("index", instance.getIndex().getValue());
    attestation.set("data", depositDataSerializer.map(instance.getDepositData()));
    return attestation;
  }
}

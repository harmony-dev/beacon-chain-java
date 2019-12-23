package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositMessage;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class DepositMessageSerializer implements ObjectSerializer<DepositMessage> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public DepositMessageSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return DepositMessage.class;
  }

  @Override
  public ObjectNode map(DepositMessage instance) {
    ObjectNode depositData = mapper.createObjectNode();
    depositData.put("pubkey", BytesValue.wrap(instance.getPubKey().getArrayUnsafe()).toString());
    depositData.put("withdrawal_credentials", instance.getWithdrawalCredentials().toString());
    depositData.set("amount", ComparableBigIntegerNode.valueOf(instance.getAmount()));
    return depositData;
  }
}

package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.state.Fork;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class DepositDataSerializer implements ObjectSerializer<DepositData> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public DepositDataSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return DepositData.class;
  }

  @Override
  public ObjectNode map(DepositData instance) {
    ObjectNode depositData = mapper.createObjectNode();
    depositData.put("pubkey", BytesValue.wrap(instance.getDepositInput().getPubKey().getArrayUnsafe()).toString());
    depositData.put("withdrawal_credentials", instance.getDepositInput().getWithdrawalCredentials().toString());
    ObjectSerializer.setUint64Field(depositData, "amount", instance.getAmount());
    depositData.put("signature", BytesValue.wrap(instance.getDepositInput().getProofOfPossession().getArrayUnsafe()).toString());
    return depositData;
  }
}

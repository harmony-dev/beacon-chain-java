package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.state.ValidatorRecord;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class ValidatorSerializer implements ObjectSerializer<ValidatorRecord> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public ValidatorSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return ValidatorRecord.class;
  }

  @Override
  public ObjectNode map(ValidatorRecord instance) {
    ObjectNode validatorRecord = mapper.createObjectNode();
    validatorRecord.put("pubkey", BytesValue.wrap(instance.getPubKey().getArrayUnsafe()).toString());
    validatorRecord.put("withdrawal_credentials", instance.getWithdrawalCredentials().toString());
//    ObjectSerializer.setUint64Field(validatorRecord, "activation_eligibility_epoch", instance.get());  TODO
    ObjectSerializer.setUint64Field(validatorRecord, "activation_epoch", instance.getActivationEpoch());
    ObjectSerializer.setUint64Field(validatorRecord, "exit_epoch", instance.getExitEpoch());
    ObjectSerializer.setUint64Field(validatorRecord, "withdrawable_epoch", instance.getWithdrawableEpoch());
    validatorRecord.put("slashed", instance.getSlashed());
//    validatorRecord.put("high_balance", instance.get()); TODO
    return validatorRecord;
  }
}

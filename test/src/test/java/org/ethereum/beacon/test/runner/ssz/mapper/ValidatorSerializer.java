package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
    validatorRecord.set("activation_eligibility_epoch", ComparableBigIntegerNode.valueOf(instance.getActivationEligibilityEpoch()));
    validatorRecord.set("activation_epoch", ComparableBigIntegerNode.valueOf(instance.getActivationEpoch()));
    validatorRecord.set("exit_epoch", ComparableBigIntegerNode.valueOf(instance.getExitEpoch()));
    validatorRecord.set("withdrawable_epoch", ComparableBigIntegerNode.valueOf(instance.getWithdrawableEpoch()));
    validatorRecord.put("slashed", instance.getSlashed());
    validatorRecord.set("effective_balance", ComparableBigIntegerNode.valueOf(instance.getEffectiveBalance()));
    return validatorRecord;
  }
}

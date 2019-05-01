package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.Transfer;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class TransferSerializer implements ObjectSerializer<Transfer> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;

  public TransferSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Class accepts() {
    return Transfer.class;
  }

  @Override
  public ObjectNode map(Transfer instance) {
    ObjectNode transfer = mapper.createObjectNode();
    ObjectSerializer.setUint64Field(transfer, "sender", instance.getSender());
    ObjectSerializer.setUint64Field(transfer, "recipient", instance.getRecipient());
    ObjectSerializer.setUint64Field(transfer, "amount", instance.getAmount());
    ObjectSerializer.setUint64Field(transfer, "fee", instance.getFee());
    ObjectSerializer.setUint64Field(transfer, "slot", instance.getSlot());
    transfer.put("pubkey", BytesValue.wrap(instance.getPubkey().getArrayUnsafe()).toString());
    transfer.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return transfer;
  }
}

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
    transfer.set("sender", ComparableBigIntegerNode.valueOf(instance.getSender()));
    transfer.set("recipient", ComparableBigIntegerNode.valueOf(instance.getRecipient()));
    transfer.set("amount", ComparableBigIntegerNode.valueOf(instance.getAmount()));
    transfer.set("fee", ComparableBigIntegerNode.valueOf(instance.getFee()));
    transfer.set("slot", ComparableBigIntegerNode.valueOf(instance.getSlot()));
    transfer.put("pubkey", BytesValue.wrap(instance.getPubkey().getArrayUnsafe()).toString());
    transfer.put("signature", BytesValue.wrap(instance.getSignature().getArrayUnsafe()).toString());
    return transfer;
  }
}

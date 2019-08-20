package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.test.type.model.BlockData;

import static org.ethereum.beacon.test.StateTestUtils.parseTransfer;

public interface TransferField extends DataMapperAccessor {
  default Transfer getTransfer() {
    final String key = "transfer.yaml";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`transfer` not defined");
    }

    try {
      BlockData.BlockBodyData.TransferData transferData =
          getMapper().readValue(getFiles().get(key), BlockData.BlockBodyData.TransferData.class);
      return parseTransfer(transferData);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

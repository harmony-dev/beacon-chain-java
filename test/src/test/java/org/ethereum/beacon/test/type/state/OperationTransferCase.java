package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.state.field.BlsSettingField;
import org.ethereum.beacon.test.type.state.field.PostField;
import org.ethereum.beacon.test.type.state.field.PreField;
import org.ethereum.beacon.test.type.state.field.TransferField;

import java.util.Map;

public class OperationTransferCase extends DataMapperTestCase
    implements BlsSettingField, PreField, PostField, TransferField {
  public OperationTransferCase(
      Map<String, String> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
  }

  @Override
  public String toString() {
    return "OperationTransferCase{" + super.toString() + '}';
  }
}

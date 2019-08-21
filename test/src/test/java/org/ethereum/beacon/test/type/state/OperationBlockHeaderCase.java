package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.DataMapperTestCase;
import org.ethereum.beacon.test.type.state.field.BlockHeaderField;
import org.ethereum.beacon.test.type.state.field.BlsSettingField;
import org.ethereum.beacon.test.type.state.field.PostField;
import org.ethereum.beacon.test.type.state.field.PreField;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;

public class OperationBlockHeaderCase extends DataMapperTestCase
    implements BlsSettingField, PreField, PostField, BlockHeaderField {
  public OperationBlockHeaderCase(
      Map<String, BytesValue> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
  }

  @Override
  public String toString() {
    return "OperationBlockHeaderCase{" + super.toString() + '}';
  }
}

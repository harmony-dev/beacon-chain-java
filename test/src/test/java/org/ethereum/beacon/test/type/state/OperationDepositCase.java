package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.DataMapperTestCase;
import org.ethereum.beacon.test.type.state.field.BlsSettingField;
import org.ethereum.beacon.test.type.state.field.DepositField;
import org.ethereum.beacon.test.type.state.field.PostField;
import org.ethereum.beacon.test.type.state.field.PreField;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;

public class OperationDepositCase extends DataMapperTestCase
    implements BlsSettingField, PreField, PostField, DepositField {
  public OperationDepositCase(
      Map<String, BytesValue> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
  }

  @Override
  public String toString() {
    return "OperationDepositCase{" + super.toString() + '}';
  }
}

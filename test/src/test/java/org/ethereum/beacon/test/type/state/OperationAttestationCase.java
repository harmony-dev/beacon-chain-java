package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.state.field.AttestationField;
import org.ethereum.beacon.test.type.state.field.BlsSettingField;
import org.ethereum.beacon.test.type.state.field.PostField;
import org.ethereum.beacon.test.type.state.field.PreField;

import java.util.Map;

public class OperationAttestationCase extends DataMapperTestCase
    implements BlsSettingField, PreField, PostField, AttestationField {
  public OperationAttestationCase(
      Map<String, String> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
  }

  @Override
  public String toString() {
    return "OperationAttestationCase{" + super.toString() + '}';
  }
}

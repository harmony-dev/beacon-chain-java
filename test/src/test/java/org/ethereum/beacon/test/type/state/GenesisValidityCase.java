package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.state.field.BlsSettingField;
import org.ethereum.beacon.test.type.state.field.GenesisField;
import org.ethereum.beacon.test.type.state.field.IsValidField;

import java.util.Map;

public class GenesisValidityCase extends DataMapperTestCase
    implements BlsSettingField, GenesisField, IsValidField {
  public GenesisValidityCase(
      Map<String, String> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
  }

  @Override
  public String toString() {
    return "GenesisValidityCase{" + super.toString() + '}';
  }
}

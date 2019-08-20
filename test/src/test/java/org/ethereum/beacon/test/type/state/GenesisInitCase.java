package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.state.field.BlsSettingField;
import org.ethereum.beacon.test.type.state.field.DepositsField;
import org.ethereum.beacon.test.type.state.field.Eth1BlockHashField;
import org.ethereum.beacon.test.type.state.field.Eth1TimestampField;
import org.ethereum.beacon.test.type.state.field.StateField;

import java.util.Map;

public class GenesisInitCase extends DataMapperTestCase
    implements BlsSettingField, StateField, DepositsField, Eth1BlockHashField, Eth1TimestampField {
  public GenesisInitCase(Map<String, String> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
  }

  @Override
  public String toString() {
    return "GenesisInitCase{" + super.toString() + '}';
  }
}

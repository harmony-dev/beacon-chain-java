package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.state.field.BlsSettingField;
import org.ethereum.beacon.test.type.state.field.PostField;
import org.ethereum.beacon.test.type.state.field.PreField;
import org.ethereum.beacon.test.type.state.field.SlotsField;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;

public class SanitySlotsCase extends DataMapperTestCase
    implements BlsSettingField, PreField, PostField, SlotsField {
  public SanitySlotsCase(Map<String, BytesValue> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
  }

  @Override
  public String toString() {
    return "SanitySlotsCase{" + super.toString() + '}';
  }
}

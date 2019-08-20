package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.state.field.BlsSettingField;
import org.ethereum.beacon.test.type.state.field.PostField;
import org.ethereum.beacon.test.type.state.field.PreField;

import java.util.Map;

public class CrosslinksProcessingCase extends DataMapperTestCase
    implements BlsSettingField, PreField, PostField {
  public CrosslinksProcessingCase(
      Map<String, String> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
  }

  @Override
  public String toString() {
    return "CrosslinksProcessingCase{" + super.toString() + '}';
  }
}

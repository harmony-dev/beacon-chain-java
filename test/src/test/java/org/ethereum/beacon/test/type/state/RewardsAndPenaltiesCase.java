package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.ethereum.beacon.test.type.DataMapperTestCase;
import org.ethereum.beacon.test.type.state.field.PostField;
import org.ethereum.beacon.test.type.state.field.PreField;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class RewardsAndPenaltiesCase extends DataMapperTestCase implements PreField, PostField {
  public RewardsAndPenaltiesCase(
      Map<String, BytesValue> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
  }

  @Override
  public String toString() {
    return "RewardsAndPenaltiesCase{" + super.toString() + '}';
  }
}

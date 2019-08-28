package org.ethereum.beacon.test.type.ssz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.DataMapperTestCase;
import org.ethereum.beacon.test.type.ssz.field.RootsField;
import org.ethereum.beacon.test.type.ssz.field.SerializedField;
import org.ethereum.beacon.test.type.ssz.field.ValueField;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;

/**
 * Ssz static tests with known containers.
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md</a>
 */
public class SszStaticCase extends DataMapperTestCase
    implements RootsField, SerializedField, ValueField {
  private final String typeName;

  public SszStaticCase(
      Map<String, BytesValue> files, ObjectMapper objectMapper, String typeName, String description) {
    super(files, objectMapper, description);
    this.typeName = typeName;
  }

  public String getTypeName() {
    return typeName;
  }

  @Override
  public String toString() {
    return "SszStaticCase{" + super.toString() + '}';
  }
}

package org.ethereum.beacon.test.type.ssz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.DataMapperTestCase;
import org.ethereum.beacon.test.type.ssz.field.MetaField;
import org.ethereum.beacon.test.type.ssz.field.SerializedField;
import org.ethereum.beacon.test.type.ssz.field.ValueField;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;

/**
 * SSZ generic test case
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic</a>
 */
public class SszGenericCase extends DataMapperTestCase
    implements MetaField, SerializedField, ValueField {
  private final String typeName;
  private final String subTypeName;
  private final boolean valid;

  public SszGenericCase(
      Map<String, BytesValue> files,
      ObjectMapper objectMapper,
      String typeName,
      String subTypeName,
      boolean valid,
      String description) {
    super(files, objectMapper, description);
    this.typeName = typeName;
    this.subTypeName = subTypeName;
    this.valid = valid;
  }

  public String getTypeName() {
    return typeName;
  }

  public String getSubTypeName() {
    return subTypeName;
  }

  public boolean isValid() {
    return valid;
  }

  @Override
  public String toString() {
    return "SszGenericCase{" + super.toString() + '}';
  }
}

package org.ethereum.beacon.test.type.state.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.ssz.SSZSerializer;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;

/**
 * Accessor of test case fields, uses set of loaded data and mapper
 */
public interface DataMapperAccessor {
  boolean USE_SSZ_WHEN_POSSIBLE = true;

  /** Data. filename: content */
  Map<String, BytesValue> getFiles();

  ObjectMapper getMapper();

  default SSZSerializer getSszSerializer() {
    return null;
  }

  void setSszSerializer(SSZSerializer sszSerializer);

  default boolean useSszWhenPossible() {
    return USE_SSZ_WHEN_POSSIBLE;
  }
}

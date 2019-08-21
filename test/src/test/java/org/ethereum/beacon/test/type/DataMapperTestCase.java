package org.ethereum.beacon.test.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.DataMapperAccessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;

public abstract class DataMapperTestCase implements TestCase, DataMapperAccessor {
  final String description;
  Map<String, BytesValue> files;
  ObjectMapper objectMapper;
  SSZSerializer sszSerializer = null;

  protected DataMapperTestCase(
      Map<String, BytesValue> files, ObjectMapper objectMapper, String description) {
    this.files = files;
    this.objectMapper = objectMapper;
    this.description = description;
  }

  @Override
  public Map<String, BytesValue> getFiles() {
    return files;
  }

  @Override
  public ObjectMapper getMapper() {
    return objectMapper;
  }

  @Override
  public SSZSerializer getSszSerializer() {
    return sszSerializer;
  }

  @Override
  public void setSszSerializer(SSZSerializer sszSerializer) {
    this.sszSerializer = sszSerializer;
  }

  @Override
  public String toString() {
    return "description='" + description + '\'';
  }
}

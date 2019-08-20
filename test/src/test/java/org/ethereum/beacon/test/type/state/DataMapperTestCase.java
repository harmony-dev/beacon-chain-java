package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.state.field.DataMapperAccessor;

import java.util.Map;

public abstract class DataMapperTestCase implements TestCase, DataMapperAccessor {
  final String description;
  Map<String, String> files;
  ObjectMapper objectMapper;

  protected DataMapperTestCase(
      Map<String, String> files, ObjectMapper objectMapper, String description) {
    this.files = files;
    this.objectMapper = objectMapper;
    this.description = description;
  }

  @Override
  public Map<String, String> getFiles() {
    return files;
  }

  @Override
  public ObjectMapper getMapper() {
    return objectMapper;
  }

  @Override
  public String toString() {
    return "description='" + description + '\'';
  }
}

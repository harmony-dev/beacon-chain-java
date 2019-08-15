package org.ethereum.beacon.test.type.state.tmp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class SanitySlotsCase implements BlsSettingField, PreField, PostField, SlotsField {

  final String description;
  Map<String, String> files;
  ObjectMapper objectMapper;

  public SanitySlotsCase(Map<String, String> files, ObjectMapper objectMapper, String description) {
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
    return "SanitySlotsCase{" + "description='" + description + '\'' + '}';
  }
}

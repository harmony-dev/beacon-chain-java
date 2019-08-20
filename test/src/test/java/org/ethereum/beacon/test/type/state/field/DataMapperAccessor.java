package org.ethereum.beacon.test.type.state.field;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public interface DataMapperAccessor {
  Map<String, String> getFiles();
  ObjectMapper getMapper();
}

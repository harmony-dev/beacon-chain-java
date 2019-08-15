package org.ethereum.beacon.test.type.state.tmp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public interface FieldLoader {
  Map<String, String> getFiles();
  ObjectMapper getMapper();
}

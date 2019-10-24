package org.ethereum.beacon.emulator.config.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Reads YAML file with keys {@link KeyData}*/
public class KeyDataReader {
  private final File file;
  private ObjectMapper mapper;

  public KeyDataReader(File file) {
    this.file = file;
    this.mapper = new ObjectMapper(new YAMLFactory());
  }

  public List<KeyData> readKeys() {
    try {
      return mapper.readValue(file, new TypeReference<List<KeyData>>(){});
    } catch (IOException e) {
      throw new RuntimeException(String.format("Error thrown when reading file %s", file), e);
    }
  }
}

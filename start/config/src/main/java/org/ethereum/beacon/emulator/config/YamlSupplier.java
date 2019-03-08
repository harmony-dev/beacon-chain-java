package org.ethereum.beacon.emulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;

/** Reads config using YAML input and Jackson FasterXML */
public class YamlSupplier implements ConfigSupplier {
  private final String data;
  private Class<? extends Config> configType;
  private ObjectMapper mapper;

  public YamlSupplier(String data, Class<? extends Config> configType) {
    this.data = data;
    this.configType = configType;
    this.mapper = new ObjectMapper(new YAMLFactory());
  }

  public Config getConfig() {
    try {
      return mapper.readValue(data, configType);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format(
              "Error thrown when reading stream with YAML config reader:\n%s", e.getMessage()),
          e);
    }
  }
}

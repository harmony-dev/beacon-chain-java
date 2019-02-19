package org.ethereum.beacon.emulator.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;

/** Presents config in YAML using Jackson FasterXML */
public class YamlPrinter implements ConfigPrinter {
  private Config config;
  private ObjectMapper mapper;

  public YamlPrinter(Config config) {
    this.config = config;
    this.mapper = new ObjectMapper(new YAMLFactory());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  @Override
  public String getString() {
    try {
      return mapper.writeValueAsString(config);
    } catch (IOException e) {
      throw new RuntimeException("Cannot read stream %s with YAML config reader", e);
    }
  }
}

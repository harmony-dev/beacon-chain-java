package org.ethereum.beacon.emulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.ethereum.beacon.emulator.config.data.Config;
import org.ethereum.beacon.emulator.config.data.ConfigVersion;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/** Reads config using YAML input and Jackson FasterXML */
public class YamlReader implements ConfigReader {
  private Map<Integer, Class<? extends Config>> handlers;
  private ObjectMapper mapper;

  public YamlReader(Map<Integer, Class<? extends Config>> handlers) {
    this.handlers = handlers;
    this.mapper = new ObjectMapper(new YAMLFactory());
  }

  public Config readConfig(Object fileObj) {
    assert fileObj instanceof File;
    File file = (File) fileObj;
    try {
      ConfigVersion configVersion = mapper.readValue(file, ConfigVersion.class);
      Class<? extends Config> handler = handlers.get(configVersion.getVersion());
      if (handler == null) {
        throw new RuntimeException(
            String.format(
                "No handler registered for YAML config version %s", configVersion.getVersion()));
      }

      return mapper.readValue(file, handler);
    } catch (IOException e) {
      throw new RuntimeException("Cannot read file %s with YAML config reader", e);
    }
  }
}

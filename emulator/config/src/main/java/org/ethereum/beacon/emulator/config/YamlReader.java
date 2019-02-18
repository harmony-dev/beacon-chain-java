package org.ethereum.beacon.emulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.ethereum.beacon.emulator.config.data.Config;
import org.ethereum.beacon.emulator.config.data.ConfigVersion;

import java.io.IOException;
import java.util.Map;

/** Reads config using YAML input and Jackson FasterXML */
public class YamlReader implements ConfigSupplier {
  private final String data;
  private Map<Integer, Class<? extends Config>> handlers;
  private ObjectMapper mapper;

  public YamlReader(String data, Map<Integer, Class<? extends Config>> handlers) {
    this.data = data;
    this.handlers = handlers;
    this.mapper = new ObjectMapper(new YAMLFactory());
  }

  public Config getConfig() {
    try {
      ConfigVersion configVersion = mapper.readValue(data, ConfigVersion.class);
      Class<? extends Config> handler = handlers.get(configVersion.getVersion());
      if (handler == null) {
        throw new RuntimeException(
            String.format(
                "No handler registered for YAML config version %s", configVersion.getVersion()));
      }

      return mapper.readValue(data, handler);
    } catch (IOException e) {
      throw new RuntimeException("Cannot read stream %s with YAML config reader", e);
    }
  }
}

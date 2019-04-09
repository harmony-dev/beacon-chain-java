package org.ethereum.beacon.emulator.config;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.ethereum.beacon.util.Objects;
import org.javatuples.Pair;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds config using several kind of users input: files and config path-value pairs All files are
 * applied in the same order as added, after that path-value pairs are applied in the order of
 * addition.
 *
 * <p>Config should be of {@link Config} kind
 */
public class ConfigBuilder<C extends Config> {
  private final Class<? extends Config> supportedConfig;
  private List<ConfigSource> configs = new ArrayList<>();
  private List<Pair<String, Object>> pathValueOverrides = new ArrayList<>();

  /**
   * Creates builder
   *
   * @param type supported config type
   */
  public ConfigBuilder(Class<C> type) {
    if (!Config.class.isAssignableFrom(type)) {
      throw new RuntimeException(
          String.format("Config type %s should implement Config interface", type));
    }
    supportedConfig = type;
  }

  /**
   * Adds Yaml config as source of configuration
   *
   * <p>NOTE: not compatible with resource files in jar, use {@link #addYamlConfig(InputStream)}
   * instead
   */
  public ConfigBuilder<C> addYamlConfig(File file) {
    InputStream inputStream = null;
    try {
      inputStream = new DataInputStream(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format("Yaml file %s was not found", file.getName()), e);
    }
    return addYamlConfig(inputStream);
  }

  /** Adds input stream with yaml config to list of config sources with guaranteed order */
  public ConfigBuilder<C> addYamlConfig(InputStream inputStream) {
    String content;
    try (InputStreamReader streamReader = new InputStreamReader(inputStream, Charsets.UTF_8)) {
      content = CharStreams.toString(streamReader);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Error reading contents of stream %s", inputStream), e);
    }
    configs.add(new YamlSource(content));
    return this;
  }

  /** A shortcut method that loads config from resource bundle */
  public ConfigBuilder<C> addYamlConfigFromResources(String resourceName) {
    return addYamlConfig(ClassLoader.class.getResourceAsStream(resourceName));
  }

  /** Adds alread created Config as source of configuration */
  public ConfigBuilder addConfig(C config) {
    configs.add(new AsIsSource(config));
    return this;
  }

  /**
   * Adds path-value pair which overrides configuration
   *
   * @param path Configuration path like "config.db.path"
   * @param value Configuraiton value, say "/home/mypath/db/"
   * @return current builder instance
   */
  public ConfigBuilder addConfigOverride(String path, Object value) {
    pathValueOverrides.add(Pair.with(path, value));
    return this;
  }

  /**
   * Same as {@link #addConfigOverride(String, Object)} but accepts Map. So order of map is
   * non-existent, but all pairs from this map will be applied after those changes that were already
   * added to the builder.
   */
  public ConfigBuilder addConfigOverrides(Map<String, String> pathValues) {
    pathValues.forEach((key, value) -> pathValueOverrides.add(Pair.with(key, value)));
    return this;
  }

  /** Same as {@link #addConfigOverride(String, Object)} but accepts List. */
  public ConfigBuilder addConfigOverrides(List<Pair<String, Object>> pathValues) {
    pathValueOverrides.addAll(pathValues);
    return this;
  }

  /**
   * Creates config from all files and pathValue overrides already added. Doesn't touch input, so
   * could be reused after new additions.
   *
   * @return built config
   */
  public C build() {
    if (configs.isEmpty()) {
      throw new RuntimeException("There should be at least one configuration provided");
    }

    // Handling config files
    ConfigSource firstConfigSource = configs.get(0);
    Config firstConfigOrig = getConfigSupplier(firstConfigSource).getConfig();
    if (!(supportedConfig.isInstance(firstConfigOrig))) {
      throw new RuntimeException(
          String.format(
              "Config is not of parameterized type %s, got instead %s",
              supportedConfig.getName(), firstConfigOrig.getClass().getName()));
    }
    C firstConfig = (C) firstConfigOrig;
    for (int i = 1; i < configs.size(); ++i) {
      ConfigSource nextConfigSource = configs.get(i);
      Config nextConfig = getConfigSupplier(nextConfigSource).getConfig();
      try {
        firstConfig = Objects.copyProperties(firstConfig, nextConfig);
      } catch (Exception ex) {
        throw new RuntimeException(
            String.format("Failed to merge config %s into main config", nextConfigSource), ex);
      }
    }

    // Handling string pathValue pairs config overrides
    for (Pair<String, Object> pathValue : pathValueOverrides) {
      try {
        Objects.setNestedProperty(firstConfig, pathValue.getValue0(), pathValue.getValue1());
      } catch (Exception e) {
        throw new RuntimeException(
            String.format(
                "Cannot override property %s with value %s",
                pathValue.getValue0(), pathValue.getValue1()),
            e);
      }
    }

    return firstConfig;
  }

  private ConfigSupplier getConfigSupplier(ConfigSource source) {
    switch (source.getType()) {
      case ASIS:
        {
          AsIsSource asIsSource = (AsIsSource) source;
          return new AsIsSupplier(asIsSource.getConfig());
        }
      case YAML:
        {
          YamlSource yamlSource = (YamlSource) source;
          return new YamlSupplier(yamlSource.getData(), supportedConfig);
        }
      default:
        {
          throw new RuntimeException(
              String.format("No handlers for config source of type %s found", source.getType()));
        }
    }
  }

  public boolean isEmpty() {
    return configs.isEmpty();
  }
}

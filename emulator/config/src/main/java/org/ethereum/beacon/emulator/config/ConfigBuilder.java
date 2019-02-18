package org.ethereum.beacon.emulator.config;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.ethereum.beacon.emulator.config.data.Config;
import org.ethereum.beacon.emulator.config.type.AsIsSource;
import org.ethereum.beacon.emulator.config.type.ConfigSource;
import org.ethereum.beacon.emulator.config.type.YamlSource;
import org.javatuples.Pair;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds config using several kind of users input: files and config path-value pairs All files are
 * applied in the same order as added, after that path-value pairs are applied in the order of
 * addition.
 *
 * <p>Config should be of {@link Config} kind
 */
public class ConfigBuilder {
  private final Map<Integer, Class<? extends Config>> supportedConfigs;
  private List<ConfigSource> configs = new ArrayList<>();
  private List<Pair<String, String>> pathValueOverrides = new ArrayList<>();

  /**
   * Creates builder
   *
   * @param types supported config types
   */
  public ConfigBuilder(Class... types) {
    Map<Integer, Class<? extends Config>> configs = new HashMap<>();
    for (Class<? extends Config> type : types) {
      if (!Config.class.isAssignableFrom(type)) {
        throw new RuntimeException(
            String.format("Config type %s should implement Config interface", type));
      }
      try {
        int version = type.newInstance().getVersion();
        configs.put(version, type);
      } catch (Exception e) {
        throw new RuntimeException(
            String.format(
                "Something goes wrong when initializing ConfigBuilder with type %s", type),
            e);
      }
    }
    this.supportedConfigs = configs;
  }

  /**
   * "copyProperties" method from <a
   * href="https://stackoverflow.com/a/24866702">https://stackoverflow.com/a/24866702</a>
   *
   * <p>Copies all properties from sources to destination, does not copy null values and any nested
   * objects will attempted to be either cloned or copied into the existing object. This is
   * recursive. Should not cause any infinite recursion.
   *
   * @param dest object to copy props into (will mutate)
   * @param sources
   * @param <T> dest
   * @return
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  private static <T> T copyProperties(T dest, Object... sources)
      throws IllegalAccessException, InvocationTargetException {
    // to keep from any chance infinite recursion lets limit each object to 1 instance at a time in
    // the stack
    final List<Object> lookingAt = new ArrayList<>();

    BeanUtilsBean recursiveBeanUtils =
        new BeanUtilsBean() {

          /**
           * Check if the class name is an internal one
           *
           * @param name
           * @return
           */
          private boolean isInternal(String name) {
            return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("com.sun.")
                || name.startsWith("javax.")
                || name.startsWith("oracle.");
          }

          /**
           * Override to ensure that we dont end up in infinite recursion
           *
           * @param dest
           * @param orig
           * @throws IllegalAccessException
           * @throws InvocationTargetException
           */
          @Override
          public void copyProperties(Object dest, Object orig)
              throws IllegalAccessException, InvocationTargetException {
            try {
              // if we have an object in our list, that means we hit some sort of recursion, stop
              // here.
              if (lookingAt.stream().anyMatch(o -> o == dest)) {
                return; // recursion detected
              }
              lookingAt.add(dest);
              super.copyProperties(dest, orig);
            } finally {
              lookingAt.remove(dest);
            }
          }

          @Override
          public void copyProperty(Object dest, String name, Object value)
              throws IllegalAccessException, InvocationTargetException {
            // dont copy over null values
            if (value != null) {
              // attempt to check if the value is a pojo we can clone using nested calls
              if (!value.getClass().isPrimitive()
                  && !value.getClass().isSynthetic()
                  && !isInternal(value.getClass().getName())) {
                try {
                  Object prop = super.getPropertyUtils().getProperty(dest, name);
                  // get current value, if its null then clone the value and set that to the value
                  if (prop == null) {
                    super.setProperty(dest, name, super.cloneBean(value));
                  } else {
                    // get the destination value and then recursively call
                    copyProperties(prop, value);
                  }
                } catch (NoSuchMethodException e) {
                  return;
                } catch (InstantiationException e) {
                  throw new RuntimeException("Nested property could not be cloned.", e);
                }
              } else {
                super.copyProperty(dest, name, value);
              }
            }
          }
        };

    for (Object source : sources) {
      recursiveBeanUtils.copyProperties(dest, source);
    }

    return dest;
  }

  /**
   * Adds Yaml config as source of configuration
   *
   * <p>NOTE: not compatible with resource files in jar, use {@link #addYamlConfig(InputStream)}
   * instead
   */
  public ConfigBuilder addYamlConfig(File file) {
    InputStream inputStream = null;
    try {
      inputStream = new DataInputStream(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format("Yaml file %s was not found", file.getName()), e);
    }
    return addYamlConfig(inputStream);
  }

  /** Adds input stream with yaml config to list of config sources with guaranteed order */
  public ConfigBuilder addYamlConfig(InputStream inputStream) {
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

  /** Adds alread created Config as source of configuration */
  public ConfigBuilder addConfig(Config config) {
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
  public ConfigBuilder addConfigOverride(String path, String value) {
    pathValueOverrides.add(Pair.with(path, value));
    return this;
  }

  /**
   * Same as {@link #addConfigOverride(String, String)} but accepts Map. So order of map is
   * non-existent, but all pairs from this map will be applied after those changes that were already
   * added to the builder.
   */
  public ConfigBuilder addConfigOverrides(Map<String, String> pathValues) {
    pathValues.forEach((key, value) -> pathValueOverrides.add(Pair.with(key, value)));
    return this;
  }

  /** Same as {@link #addConfigOverride(String, String)} but accepts List. */
  public ConfigBuilder addConfigOverrides(List<Pair<String, String>> pathValues) {
    pathValueOverrides.addAll(pathValues);
    return this;
  }

  /**
   * Creates config from all files and pathValue overrides already added. Doesn't touch input, so
   * could be reused after new additions.
   *
   * @return built config
   */
  public Config build() {
    if (configs.isEmpty()) {
      throw new RuntimeException("There should be at least one configuration provided");
    }

    // Handling config files
    ConfigSource firstConfigSource = configs.get(0);
    Config firstConfig = getConfigSupplier(firstConfigSource).getConfig();
    int version = firstConfig.getVersion();

    for (int i = 1; i < configs.size(); ++i) {
      ConfigSource nextConfigSource = configs.get(i);
      Config nextConfig = getConfigSupplier(nextConfigSource).getConfig();
      if (nextConfig.getVersion() != version) {
        throw new RuntimeException(
            String.format(
                "All configs should have the same version. First config has %s version, "
                    + "current config %s has version %s.",
                version, nextConfigSource, nextConfig.getVersion()));
      }
      try {
        firstConfig = copyProperties(firstConfig, nextConfig);
      } catch (Exception ex) {
        throw new RuntimeException(
            String.format("Failed to merge config %s into main config", nextConfigSource), ex);
      }
    }

    // Handling string pathValue pairs config overrides
    for (Pair<String, String> pathValue : pathValueOverrides) {
      try {
        PropertyUtils.setNestedProperty(firstConfig, pathValue.getValue0(), pathValue.getValue1());
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

          return new YamlReader(yamlSource.getData(), supportedConfigs);
        }
      default:
        {
          throw new RuntimeException(
              String.format("No handlers for config source of type %s found", source.getType()));
        }
    }
  }
}

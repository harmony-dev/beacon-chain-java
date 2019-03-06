package org.ethereum.beacon;

import org.apache.logging.log4j.Level;
import org.ethereum.beacon.emulator.config.Config;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.YamlPrinter;
import org.ethereum.beacon.emulator.config.chainspec.Spec;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.javatuples.Pair;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReusableOptions {

  static final String VERSION = "0.1b";
  private static final Map<LogLevel, Level> LOG_LEVELS = new HashMap<>();

  static {
    LOG_LEVELS.put(LogLevel.OFF, Level.OFF);
    LOG_LEVELS.put(LogLevel.ERROR, Level.ERROR);
    LOG_LEVELS.put(LogLevel.INFO, Level.INFO);
    LOG_LEVELS.put(LogLevel.DEBUG, Level.DEBUG);
    LOG_LEVELS.put(LogLevel.ALL, Level.ALL);
  }

  @CommandLine.Option(
      names = {"-c", "--config"},
      paramLabel = "CONFIG",
      description =
          "YAML config file.\nAll options that are not set will be loaded from default config.")
  File[] configs;

  @CommandLine.Option(
      names = {"-cs", "--save-config"},
      paramLabel = "SAVE-CONFIG",
      description = "Saves merged configuration to file in YAML format")
  File config;

  @CommandLine.Option(
      names = {"-s", "--spec"},
      paramLabel = "CHAINSPEC",
      description =
          "YAML chain specification file.\nFor all specifications that are not set, default values are used.")
  File[] chainspecs;

  @CommandLine.Option(
      names = {"-ss", "--save-spec"},
      paramLabel = "SAVE-SPEC",
      description = "Saves merged chain specification to file in YAML format")
  File chainspec;

  @CommandLine.Option(
      names = {"-l", "--loglevel"},
      paramLabel = "LOG",
      description =
          "Sets log level in the range from OFF to ALL messages, default is INFO. Available levels: ${COMPLETION-CANDIDATES}")
  LogLevel logLevel;

  @CommandLine.Option(names = "-v")
  boolean verbose;

  List<Pair<String, Object>> configPathValues = new ArrayList<>();
  List<Pair<String, Object>> chainSpecPathValues = new ArrayList<>();

  private MainConfig prepareConfig(String extraConfig) {
    ConfigBuilder<MainConfig> configBuilder = new ConfigBuilder<>(MainConfig.class);
    configBuilder.addYamlConfig(
        ClassLoader.class.getResourceAsStream("/config/default-config.yml"));
    configBuilder.addYamlConfig(ClassLoader.class.getResourceAsStream(extraConfig));
    if (configs != null) {
      for (File config : configs) {
        configBuilder.addYamlConfig(config);
      }
    }

    for (Pair<String, Object> pathValue : configPathValues) {
      configBuilder.addConfigOverride(pathValue.getValue0(), pathValue.getValue1());
    }

    return configBuilder.build();
  }

  private Spec prepareChainSpec(@Nullable String extraChainSpec) {
    ConfigBuilder<Spec> configBuilder = new ConfigBuilder<>(Spec.class);
    configBuilder.addYamlConfig(
        ClassLoader.class.getResourceAsStream("/config/default-chainSpec.yml"));
    if (extraChainSpec != null) {
      configBuilder.addYamlConfig(ClassLoader.class.getResourceAsStream(extraChainSpec));
    }
    if (chainspecs != null) {
      for (File chainspec : chainspecs) {
        configBuilder.addYamlConfig(chainspec);
      }
    }

    for (Pair<String, Object> pathValue : chainSpecPathValues) {
      configBuilder.addConfigOverride(pathValue.getValue0(), pathValue.getValue1());
    }

    return configBuilder.build();
  }

  Pair<MainConfig, Spec> prepareAndPrintConfigs(Task action, String extraConfig) {
    return prepareAndPrintConfigs(action, extraConfig, null);
  }

  Pair<MainConfig, Spec> prepareAndPrintConfigs(
      Task action, String extraConfig, @Nullable String extraChainSpec) {
    MainConfig mainConfig = prepareConfig(extraConfig);
    Spec specConstantsData = prepareChainSpec(extraChainSpec);

    // Print if needed
    if (action.equals(Task.config)) {
      System.out.println("If executed with " + Task.run + " command, will use following options.");
      System.out.println("Main config:");
      System.out.println(new YamlPrinter(mainConfig).getString());
      if (verbose) {
        System.out.println("Spec constants:");
        System.out.println(new YamlPrinter(specConstantsData).getString());
      } else {
        System.out.println("To see chain specifications use verbose `-v` option.");
      }
    }

    // Save if needed
    if (config != null) {
      System.out.println("Saving config to file: " + config);
      saveConfigToFile(mainConfig, config);
    }
    if (chainspec != null) {
      System.out.println("Saving chain specification to file: " + chainspec);
      saveConfigToFile(specConstantsData, chainspec);
    }

    return Pair.with(mainConfig, specConstantsData);
  }

  // Overrides without prompt
  void saveConfigToFile(Config config, File file) {
    try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
      out.print(new YamlPrinter(config).getString());
      out.flush();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format("Cannot save to file %s", file), e);
    }
  }

  Level prepareLogLevel(boolean print) {
    if (logLevel != null) {
      if (print) {
        System.out.println("Setting log level to " + logLevel);
      }
      return LOG_LEVELS.get(logLevel);
    }

    return null;
  }

  enum Task {
    run,
    config
  }

  enum LogLevel {
    OFF,
    ERROR,
    INFO,
    DEBUG,
    ALL
  }
}

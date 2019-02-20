package org.ethereum.beacon;

import org.apache.logging.log4j.Level;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.YamlPrinter;
import org.ethereum.beacon.emulator.config.chainspec.ChainSpecData;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.javatuples.Pair;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReusableOptions {

  static final String VERSION = "0.1b";
  private static final Map<Integer, Level> LOG_LEVELS = new HashMap<>();

  static {
    LOG_LEVELS.put(0, Level.OFF);
    LOG_LEVELS.put(1, Level.FATAL);
    LOG_LEVELS.put(2, Level.ERROR);
    LOG_LEVELS.put(3, Level.WARN);
    LOG_LEVELS.put(4, Level.INFO);
    LOG_LEVELS.put(5, Level.DEBUG);
    LOG_LEVELS.put(6, Level.TRACE);
    LOG_LEVELS.put(7, Level.ALL);
  }

  @CommandLine.Option(
      names = {"-c", "--config"},
      paramLabel = "CONFIG",
      description =
          "YAML config file.\nAll options that are not set will be loaded from default config.")
  File[] configs;
  @CommandLine.Option(
      names = {"-s", "--spec"},
      paramLabel = "CHAINSPEC",
      description =
          "YAML chain specification file.\nFor all specifications that are not set, default values are used.")
  File[] chainspecs;
  @CommandLine.Option(
      names = {"-l", "--loglevel"},
      paramLabel = "LOG",
      description = "Sets log level in the range of 0..7 (OFF..ALL), default is 4 (INFO).")
  Integer logLevel;
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

  private ChainSpecData prepareChainSpec(@Nullable String extraChainSpec) {
    ConfigBuilder<ChainSpecData> configBuilder = new ConfigBuilder<>(ChainSpecData.class);
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

  Pair<MainConfig, ChainSpecData> prepareAndPrintConfigs(Task action, String extraConfig) {
    return prepareAndPrintConfigs(action, extraConfig, null);
  }

  Pair<MainConfig, ChainSpecData> prepareAndPrintConfigs(
      Task action, String extraConfig, @Nullable String extraChainSpec) {
    MainConfig mainConfig = prepareConfig(extraConfig);
    ChainSpecData chainSpecData = prepareChainSpec(extraChainSpec);
    if (action.equals(Task.config)) {
      System.out.println("If executed with " + Task.run + " command, will use following options.");
      System.out.println("Main config:");
      System.out.println(new YamlPrinter(mainConfig).getString());
      if (verbose) {
        System.out.println("Chain specifications:");
        System.out.println(new YamlPrinter(chainSpecData).getString());
      } else {
        System.out.println("To see chain specifications use verbose `-v` option.");
      }
    }

    return Pair.with(mainConfig, chainSpecData);
  }

  Level prepareLogLevel(boolean print) {
    if (logLevel != null) {
      if (print) {
        System.out.println("Setting log level to " + logLevel);
      }
      Level res;
      if (logLevel >= 7) {
        res = LOG_LEVELS.get(7);
      } else {
        res = LOG_LEVELS.get(logLevel);
      }

      return res;
    }

    return null;
  }

  enum Task {
    run,
    config
  }
}

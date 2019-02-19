package org.ethereum.beacon;

import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.YamlPrinter;
import org.ethereum.beacon.emulator.config.chainspec.ChainSpecData;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.javatuples.Pair;
import picocli.CommandLine;

import java.io.File;

public class ReusableOptions {

  static final String VERSION = "0.1b";

  @CommandLine.Option(names = { "-c", "--config" }, paramLabel = "CONFIG", description = "YAML config file.\nAll options that are not set will be loaded from default config.")
  File[] configs;

  @CommandLine.Option(names = { "-s", "--spec" }, paramLabel = "CHAINSPEC", description = "YAML chain specification file.\nFor all specifications that are not set, default values are used.")
  File[] chainspecs;

  MainConfig prepareConfig(String extraConfig) {
    ConfigBuilder<MainConfig> configBuilder = new ConfigBuilder<>(MainConfig.class);
    configBuilder.addYamlConfig(ClassLoader.class.getResourceAsStream("/config/default-config.yml"));
    configBuilder.addYamlConfig(ClassLoader.class.getResourceAsStream(extraConfig));
    if (configs != null) {
      for (File config : configs) {
        configBuilder.addYamlConfig(config);
      }
    }
    return configBuilder.build();

  }

  ChainSpecData prepareChainSpec() {
    ConfigBuilder<ChainSpecData> configBuilder = new ConfigBuilder<>(ChainSpecData.class);
    configBuilder.addYamlConfig(ClassLoader.class.getResourceAsStream("/config/default-chainSpec.yml"));
    if (chainspecs != null) {
      for (File chainspec : chainspecs) {
        configBuilder.addYamlConfig(chainspec);
      }
    }
    return configBuilder.build();
  }

  Pair<MainConfig, ChainSpecData> prepareConfigs(Task action, String extraConfig) {
    MainConfig mainConfig = prepareConfig(extraConfig);
    ChainSpecData chainSpecData = prepareChainSpec();
    if (action.equals(Task.print)) {
      System.out.println("If executed with `start` command, will use following options.");
      System.out.println("Main config:");
      System.out.println(new YamlPrinter(mainConfig).getString());
      System.out.println("Chain specifications:");
      System.out.println(new YamlPrinter(chainSpecData).getString());
    } else {
      throw new RuntimeException("Not implemented yet");
    }

    return Pair.with(mainConfig, chainSpecData);
  }

  enum Task {
    start,
    print
  }
}


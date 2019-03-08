package org.ethereum.beacon;

import java.io.File;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.Simulator.SimulatorPrintSpec;
import org.ethereum.beacon.Simulator.SimulatorRun;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.YamlPrinter;
import org.ethereum.beacon.emulator.config.chainspec.Spec;
import org.ethereum.beacon.emulator.config.simulator.SimulatorConfig;
import picocli.CommandLine;

@CommandLine.Command(
    description = "Eth2.0 beacon chain simulator",
    name = "simulator",
    version = "simulator " + ReusableOptions.VERSION,
    mixinStandardHelpOptions = true,
    subcommands = { SimulatorRun.class, SimulatorPrintSpec.class })
public class Simulator implements Callable<Void> {

  private static final Logger logger = LogManager.getLogger("simulator");

  @CommandLine.Command(
      name = "run",
      description = "Runs simulation",
      mixinStandardHelpOptions = true)
  public static class SimulatorRun extends ReusableOptions implements Callable<Void>  {

    @Override
    public Void call() throws Exception {
      ConfigBuilder<SimulatorConfig> configBuilder = new ConfigBuilder<>(SimulatorConfig.class);
      if (this.configs != null) {
        for (File config : this.configs) {
          configBuilder.addYamlConfig(config);
        }
      } else {
        logger.info("Simulator config is not set, fallback to default configuration");
        configBuilder.addYamlConfig(ClassLoader.class.getResourceAsStream("/config/default-simulation.yml"));
      }

      SimulatorConfig simulatorConfig = configBuilder.build();
      Spec spec = prepareChainSpec("/config/simulator-chainSpec.yml");

      SimulatorLauncher simulatorLauncher =
          new SimulatorLauncher(
              simulatorConfig,
              spec.buildSpecHelpers(simulatorConfig.isBlsVerifyEnabled()),
              prepareLogLevel(false));
      simulatorLauncher.run();

      return null;
    }
  }

  @CommandLine.Command(
      name = "spec",
      description = "Prints default spec constants used by simulator",
      mixinStandardHelpOptions = true)
  public static class SimulatorPrintSpec implements Callable<Void> {
    @Override
    public Void call() throws Exception {
      ConfigBuilder<Spec> configBuilder = new ConfigBuilder<>(Spec.class);
      configBuilder.addYamlConfig(
          ClassLoader.class.getResourceAsStream("/config/default-chainSpec.yml"));
      configBuilder.addYamlConfig(
          ClassLoader.class.getResourceAsStream("/config/simulator-chainSpec.yml"));
      System.out.println(new YamlPrinter(configBuilder.build()).getString());
      return null;
    }
  }

  public static void main(String[] args) {
    try {
      CommandLine.call(new Simulator(), args);
    } catch (Exception e) {
      System.out.println(String.format((char) 27 + "[31m" + "FATAL ERROR: %s", e.getMessage()));
    }
  }

  @Override
  public Void call() throws Exception {
    CommandLine.usage(this, System.out);
    return null;
  }
}

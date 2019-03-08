package org.ethereum.beacon;

import java.util.concurrent.Callable;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.Spec;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.simulator.SimulatorConfig;
import org.javatuples.Pair;
import picocli.CommandLine;

@CommandLine.Command(
    description = "Eth2.0 beacon chain simulator",
    name = "simulator",
    mixinStandardHelpOptions = true,
    version = "simulator " + ReusableOptions.VERSION)
public class Simulator extends ReusableOptions implements Callable<Void> {

  @CommandLine.Parameters(
      index = "0",
      description =
          "Task to do: run/config.\n run - Runs beacon simulator.\n config - Prints configuration and tasks to run on start.")
  Task action;

  public static void main(String[] args) {
    try {
      CommandLine.call(new Simulator(), args);
    } catch (Exception e) {
      System.out.println(String.format((char) 27 + "[31m" + "FATAL ERROR: %s", e.getMessage()));
    }
  }

  @Override
  public Void call() throws Exception {
    Pair<MainConfig, Spec> configs =
        prepareAndPrintConfigs(action, "/config/simulator-config.yml", "/config/simulator-chainSpec.yml");

    SimulatorConfig simulatorConfig =
        new ConfigBuilder<>(SimulatorConfig.class)
            .addYamlConfig(ClassLoader.class.getResourceAsStream("/config/default-simulation.yml"))
            .build();

    if (action.equals(Task.run)) {
      SimulatorLauncher simulatorLauncher =
          new SimulatorLauncher(
              simulatorConfig,
              configs.getValue1().buildSpecHelpers(simulatorConfig.isBlsVerifyEnabled()),
              prepareLogLevel(true));
      simulatorLauncher.run();
    }

    return null;
  }
}

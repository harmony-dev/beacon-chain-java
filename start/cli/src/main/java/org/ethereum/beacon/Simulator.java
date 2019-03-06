package org.ethereum.beacon;

import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.javatuples.Pair;
import picocli.CommandLine;

import java.util.concurrent.Callable;

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

  @CommandLine.Parameters(
      index = "1",
      description = "Number of validators to simulate.",
      arity = "0..1")
  Integer validators;

  public static void main(String[] args) {
    try {
      CommandLine.call(new Simulator(), args);
    } catch (Exception e) {
      System.out.println(String.format((char) 27 + "[31m" + "FATAL ERROR: %s", e.getMessage()));
    }
  }

  @Override
  public Void call() throws Exception {
    System.out.println("Starting beacon simulator...");
    if (validators != null) {
      configPathValues.add(Pair.with("plan.validator[0].count", validators));
    }
    Pair<MainConfig, SpecConstantsData> configs =
        prepareAndPrintConfigs(action, "/config/simulator-config.yml", "/config/simulator-chainSpec.yml");

    if (action.equals(Task.run)) {
      SimulatorLauncher simulatorLauncher =
          new SimulatorLauncher(
              configs.getValue0(),
              configs.getValue1().buildSpecHelpers(),
              prepareLogLevel(true),
              mainConfig -> {
                if (config != null) {
                  System.out.println("Updating config to file: " + config);
                  saveConfigToFile(mainConfig, config);
                }
              });
      simulatorLauncher.run();
    }

    return null;
  }
}

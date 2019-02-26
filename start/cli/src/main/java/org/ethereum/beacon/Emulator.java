package org.ethereum.beacon;

import org.ethereum.beacon.emulator.config.chainspec.ChainSpecData;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.javatuples.Pair;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    description = "Eth2.0 beacon emulator",
    name = "emulator",
    mixinStandardHelpOptions = true,
    version = "emulator " + ReusableOptions.VERSION)
public class Emulator extends ReusableOptions implements Callable<Void> {

  @CommandLine.Parameters(
      index = "0",
      description =
          "Task to do: run/config.\n run - Runs beacon emulator.\n config - Prints configuration and tasks to run on start.")
  Task action;

  @CommandLine.Parameters(
      index = "1",
      description = "Number of validators to emulate.",
      arity = "0..1")
  Integer validators;

  public static void main(String[] args) {
    try {
      CommandLine.call(new Emulator(), args);
    } catch (Exception e) {
      System.out.println(String.format((char) 27 + "[31m" + "FATAL ERROR: %s", e.getMessage()));
    }
  }

  @Override
  public Void call() throws Exception {
    System.out.println("Starting beacon emulator...");
    if (validators != null) {
      configPathValues.add(Pair.with("plan.validator[0].count", validators));
    }
    Pair<MainConfig, ChainSpecData> configs =
        prepareAndPrintConfigs(action, "/config/emulator-config.yml");

    if (action.equals(Task.run)) {
      EmulatorLauncher emulatorLauncher =
          new EmulatorLauncher(
              configs.getValue0(),
              configs.getValue1().build(),
              prepareLogLevel(true),
              mainConfig -> {
                if (config != null) {
                  System.out.println("Updating config to file: " + config);
                  saveConfigToFile(mainConfig, config);
                }
              });
      emulatorLauncher.run();
    }

    return null;
  }
}

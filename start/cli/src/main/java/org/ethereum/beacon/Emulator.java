package org.ethereum.beacon;

import org.ethereum.beacon.emulator.config.chainspec.ChainSpecData;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.javatuples.Pair;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    description = "Eth2.0 emulator",
    name = "beacon-emulator",
    mixinStandardHelpOptions = true,
    version = "beacon-emulator " + ReusableOptions.VERSION)
public class Emulator extends ReusableOptions implements Callable<Void> {
  @CommandLine.Parameters(
      index = "0",
      description = "Number of validators to emulate.",
      arity = "1")
  Integer validators;

  @CommandLine.Parameters(
      index = "1",
      description =
          "Task to do: run/config.\n run - Runs beacon emulator.\n config - Prints configuration and tasks to run on start.")
  Task action;


  public static void main(String[] args) {
    CommandLine.call(new Emulator(), args);
  }

  @Override
  public Void call() throws Exception {
    System.out.println("Starting beacon emulator...");
    if (validators != null) {
      configPathValues.add(Pair.with("plan.validator[0].count", validators));
    }
    Pair<MainConfig, ChainSpecData> configs =
        prepareAndPrintConfigs(
            action, "/config/emulator-config.yml");

    if (action.equals(Task.run)) {
      EmulatorLauncher emulatorLauncher =
          new EmulatorLauncher(configs.getValue0(), configs.getValue1().build(), prepareLogLevel(true));
      emulatorLauncher.run();
    }

    return null;
  }
}

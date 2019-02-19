package org.ethereum.beacon;

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
      description =
          "Task to do: start/print.\n start - Starts beacon emulator.\n print - Prints configuration and tasks to run on start.")
  Task action;

  public static void main(String[] args) {
    CommandLine.call(new Emulator(), args);
  }

  @Override
  public Void call() throws Exception {
    // TODO: your business logic goes here...
    System.out.println("Starting beacon emulator...");
    prepareConfigs(action, "/config/emulator-config.yml");
    return null;
  }
}

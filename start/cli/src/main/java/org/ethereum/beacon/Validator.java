package org.ethereum.beacon;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    description = "Eth2.0 validator",
    name = "beacon-validator",
    mixinStandardHelpOptions = true,
    version = "beacon-validator " + ReusableOptions.VERSION)
public class Validator extends ReusableOptions implements Callable<Void> {
  @CommandLine.Parameters(
      index = "0",
      description =
          "Task to do: start/print.\n start - Starts beacon validator.\n print - Prints configuration and tasks to run on start.")
  Task action;

  public static void main(String[] args) {
    CommandLine.call(new Validator(), args);
  }

  @Override
  public Void call() throws Exception {
    // TODO: your business logic goes here...
    System.out.println("Starting beacon validator...");
    prepareAndPrintConfigs(action, "/config/validator-config.yml");
    return null;
  }
}

package org.ethereum.beacon;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    description = "Eth2.0 beacon chain client",
    name = "beacon-chainer",
    mixinStandardHelpOptions = true,
    version = "beacon-chainer " + ReusableOptions.VERSION)
public class BeaconChainer extends ReusableOptions implements Callable<Void> {
  @CommandLine.Parameters(
      index = "0",
      description =
          "Task to do: start/print.\n start - Starts beacon chain.\n print - Prints configuration and tasks to run on start.")
  Task action;

  public static void main(String[] args) {
    CommandLine.call(new BeaconChainer(), args);
  }

  @Override
  public Void call() throws Exception {
    System.out.println("Starting beacon chain client...");
    prepareConfigs(action, "/config/chainer-config.yml");
    // TODO: your business logic goes here...
    return null;
  }
}

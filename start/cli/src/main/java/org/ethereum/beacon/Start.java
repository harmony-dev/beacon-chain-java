package org.ethereum.beacon;

import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(
    description = "Eth2.0 beacon chain client",
    name = "beacon-chain",
    mixinStandardHelpOptions = true,
    version = "beacon-chain 0.1b")
public class Start implements Callable<Void> {

  @CommandLine.Parameters(index = "0", description = "YAML config.")
  private File file;

  public static void main(String[] args) {
    CommandLine.call(new Start(), args);
  }

  @Override
  public Void call() throws Exception {
    // TODO: your business logic goes here...
    System.out.println("Starting...");
    return null;
  }
}

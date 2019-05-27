package org.ethereum.beacon.node;

import org.ethereum.beacon.node.command.RunNode;
import picocli.CommandLine;
import picocli.CommandLine.RunLast;

@CommandLine.Command(
    description = "Beacon chain node",
    name = "node",
    version = "node " + Node.VERSION,
    mixinStandardHelpOptions = true,
    subcommands = {RunNode.class})
public class Node implements Runnable {

  static final String VERSION = "0.1.0";

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_EXIT_CODE = 1;

  public static void main(String[] args) {
    try {
      CommandLine commandLine = new CommandLine(new Node());
      commandLine.setCaseInsensitiveEnumValuesAllowed(true);
      commandLine.parseWithHandlers(
          new RunLast().andExit(SUCCESS_EXIT_CODE),
          CommandLine.defaultExceptionHandler().andExit(ERROR_EXIT_CODE),
          args);
    } catch (Exception e) {
      System.out.println(String.format((char) 27 + "[31m" + "FATAL ERROR: %s", e.getMessage()));
    }
  }

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }
}

package org.ethereum.beacon.simulator;

import org.ethereum.beacon.simulator.Simulator.VersionProvider;
import org.ethereum.beacon.simulator.command.PrintSpec;
import org.ethereum.beacon.simulator.command.RunSimulation;
import org.ethereum.beacon.start.common.ClientInfo;
import picocli.CommandLine;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.RunLast;

@CommandLine.Command(
    description = "Beacon chain simulator",
    name = "simulator",
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true,
    subcommands = {RunSimulation.class, PrintSpec.class})
public class Simulator implements Runnable {

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_EXIT_CODE = 1;

  public static void main(String[] args) {
    try {
      CommandLine commandLine = new CommandLine(new Simulator());
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

  static class VersionProvider implements IVersionProvider {
    @Override
    public String[] getVersion() throws Exception {
      return new String[] { "simulator " + ClientInfo.version() };
    }
  }
}

package org.ethereum.beacon.simulator;

import org.ethereum.beacon.simulator.BenchmarkLauncher.Builder;
import org.ethereum.beacon.simulator.command.LogLevel;
import picocli.CommandLine;
import picocli.CommandLine.RunLast;

@CommandLine.Command(
    description = "Beacon chain simulator",
    name = "benchrunner",
    version = "benchrunner" + BenchRunner.VERSION,
    mixinStandardHelpOptions = true)
public class BenchRunner implements Runnable {

  static final String VERSION = "0.1.0";

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_EXIT_CODE = 1;

  public static void main(String[] args) {
    try {
      CommandLine commandLine = new CommandLine(new BenchRunner());
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
    new Builder()
        .withLogLevel(LogLevel.info.toLog4j())
        .withConfigFromResource("/config/default-simulation-config.yml")
        .build()
        .run();
  }
}

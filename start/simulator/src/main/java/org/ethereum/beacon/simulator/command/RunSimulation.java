package org.ethereum.beacon.simulator.command;

import java.io.File;
import org.ethereum.beacon.simulator.SimulatorLauncher;
import org.ethereum.beacon.simulator.SimulatorLauncher.Builder;
import picocli.CommandLine;

@CommandLine.Command(name = "run", description = "Runs simulation", mixinStandardHelpOptions = true)
public class RunSimulation implements Runnable {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "simulation-config.yml",
      description =
          "A path to a config file containing simulation plan in YAML format\nuse 'default' to run a simulation with default setup")
  private String config;

  @CommandLine.Option(
      names = {"--loglevel"},
      paramLabel = "level",
      description = "Log verbosity level: all, debug, info, error\ninfo is set by default")
  private LogLevel logLevel = LogLevel.info;

  @Override
  public void run() {
    SimulatorLauncher.Builder simulationBuilder = new Builder().withLogLevel(logLevel.toLog4j());

    if ("default".equals(config)) {
      simulationBuilder
          .withConfigFromResource("/config/default-simulation-config.yml");
    } else {
      simulationBuilder.withConfigFromFile(new File(config));
    }

    simulationBuilder
        .withNewDataProcessor(true)
        .build()
        .run();
  }
}

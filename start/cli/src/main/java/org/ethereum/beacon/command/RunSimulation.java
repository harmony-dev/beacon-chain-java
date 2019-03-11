package org.ethereum.beacon.command;

import java.io.File;
import org.ethereum.beacon.SimulatorLauncher;
import org.ethereum.beacon.SimulatorLauncher.Builder;
import picocli.CommandLine;

@CommandLine.Command(name = "run", description = "Runs simulation", mixinStandardHelpOptions = true)
public class RunSimulation implements Runnable {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "plan",
      description =
          "A path to a simulation plan in YAML format\npass 'default' to run a simulation with default setup")
  private String plan;

  @CommandLine.Option(
      names = {"--spec"},
      paramLabel = "spec.yml",
      description =
          "A path to YAML file with beacon chain constants\nvalues from given file override values from the spec")
  private File spec;

  @CommandLine.Option(
      names = {"--loglevel"},
      paramLabel = "level",
      description = "Log verbosity level: all, debug, info, error\ninfo is set by default")
  private LogLevel logLevel = LogLevel.info;

  @Override
  public void run() {
    SimulatorLauncher.Builder simulationBuilder = new Builder().withLogLevel(logLevel.toLog4j());

    if ("default".equals(plan)) {
      simulationBuilder
          .withPlanFromResource("/config/default-simulation.yml")
          .addSpecFromResource("/config/default-simulation-constants.yml");
    } else {
      simulationBuilder.withPlanFromFile(new File(plan));
      if (spec != null) {
        simulationBuilder.addSpecFromFile(spec);
      }
    }

    simulationBuilder.build().run();
  }
}

package org.ethereum.beacon.command;

import java.io.File;
import org.ethereum.beacon.SimulatorLauncher;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.Spec;
import org.ethereum.beacon.emulator.config.simulator.SimulatorConfig;
import picocli.CommandLine;

@CommandLine.Command(
    name = "run",
    description = "Runs simulation",
    mixinStandardHelpOptions = true,
    subcommands = {RunDefaultSimulation.class})
public class RunSimulation implements Runnable {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "plan",
      description = "A path to a simulation plan in YAML format")
  private File plan;

  @CommandLine.Option(
      names = {"--spec"},
      paramLabel = "spec.yml",
      description =
          "A path to YAML file with beacon chain constants\nvalues from given file override values from the spec")
  private File specOverrides;

  @CommandLine.Option(
      names = {"--loglevel"},
      paramLabel = "level",
      description = "Log verbosity level: all, debug, info, error\ninfo is set by default")
  private LogLevel logLevel = LogLevel.info;

  @Override
  public void run() {
    SimulatorConfig simulationPlan =
        new ConfigBuilder<>(SimulatorConfig.class).addYamlConfig(plan).build();

    ConfigBuilder<Spec> specBuilder =
        new ConfigBuilder<>(Spec.class).addYamlConfigFromResources("/config/spec-constants.yml");
    if (specOverrides != null) {
      specBuilder.addYamlConfig(specOverrides);
    }
    Spec spec = specBuilder.build();

    new SimulatorLauncher(
            simulationPlan,
            spec.buildSpecHelpers(simulationPlan.isBlsVerifyEnabled()),
            logLevel.toLog4j())
        .run();
  }
}

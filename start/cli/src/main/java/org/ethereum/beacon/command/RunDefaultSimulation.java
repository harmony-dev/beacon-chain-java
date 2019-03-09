package org.ethereum.beacon.command;

import org.ethereum.beacon.SimulatorLauncher;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.Spec;
import org.ethereum.beacon.emulator.config.simulator.SimulatorConfig;
import picocli.CommandLine;

@CommandLine.Command(
    name = "default",
    description = "Runs simulation with default setup",
    mixinStandardHelpOptions = true)
public class RunDefaultSimulation implements Runnable {

  @CommandLine.Option(
      names = {"--loglevel"},
      paramLabel = "level",
      description = "Log verbosity level: ${COMPLETION-CANDIDATES}\n is set by default")
  private LogLevel logLevel = LogLevel.info;

  @Override
  public void run() {
    SimulatorConfig simulatorConfig =
        new ConfigBuilder<>(SimulatorConfig.class)
            .addYamlConfigFromResources("/config/default-simulation.yml")
            .build();

    Spec spec =
        new ConfigBuilder<>(Spec.class)
            .addYamlConfigFromResources("/config/spec-constants.yml")
            .addYamlConfigFromResources("/config/default-simulation-constants.yml")
            .build();

    new SimulatorLauncher(
            simulatorConfig,
            spec.buildSpecHelpers(simulatorConfig.isBlsVerifyEnabled()),
            logLevel.toLog4j())
        .run();
  }
}

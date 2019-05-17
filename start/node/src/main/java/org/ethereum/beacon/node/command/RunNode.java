package org.ethereum.beacon.node.command;

import java.io.File;
import org.ethereum.beacon.node.NodeCommandLauncher;
import picocli.CommandLine;

@CommandLine.Command(name = "run", description = "Runs beacon chain node", mixinStandardHelpOptions = true)
public class RunNode implements Runnable {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "node-config.yml",
      description =
          "A path to a config file containing node config in YAML format\nuse 'default' to run a node with default setup")
  private String config;

  @CommandLine.Option(
      names = {"--loglevel"},
      paramLabel = "level",
      description = "Log verbosity level: all, debug, info, error\ninfo is set by default")
  private LogLevel logLevel = LogLevel.info;

  @Override
  public void run() {
    NodeCommandLauncher.Builder nodeBuilder = new NodeCommandLauncher.Builder().withLogLevel(logLevel.toLog4j());

    if ("default".equals(config)) {
      nodeBuilder.withConfigFromResource("/config/default-node-config.yml");
    } else {
      nodeBuilder.withConfigFromFile(new File(config));
    }

    nodeBuilder.build().run();
  }
}

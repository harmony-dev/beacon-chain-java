package org.ethereum.beacon.node.command;

import java.io.File;
import java.util.List;
import org.ethereum.beacon.node.NodeCommandLauncher;
import picocli.CommandLine;

@CommandLine.Command(
  name = "run",
  description = "Runs beacon chain node",
  mixinStandardHelpOptions = true,
  sortOptions = false
)
public class RunNode implements Runnable {

  @CommandLine.Parameters(
    index = "0",
    paramLabel = "node-config.yml",
    description =
        "A path to a config file containing node config in YAML format\nuse 'default' to run a node with default setup"
  )
  private String config;

  @CommandLine.Option(
    names = {"--loglevel"},
    paramLabel = "level",
    description = "Log verbosity level: all, debug, info, error\ninfo is set by default"
  )
  private LogLevel logLevel = null;

  @CommandLine.Option(
    names = {"--listen"},
    paramLabel = "port",
    description = "Listen for inbound connections on TCP port"
  )
  private Integer listenPort;

  @CommandLine.Option(
    names = {"--connect"},
    paramLabel = "URL",
    split = ",",
    description = "Actively connects to remote peers. URL in form 'tcp://<host>:<port>'"
  )
  private List<String> activePeers;

  @CommandLine.Option(
    names = {"--validators"},
    paramLabel = "key",
    split = ",",
    description = {
      "List of signers. Entry is either hex private key (starting from '0x'), "
          + "or index of keypair specified in the 'contract emulator' config deposits "
          + "or a range of such indices",
      "Example: --validators=1,2,5-9,0x1234567...ef"
    }
  )
  private List<String> validators;

  @CommandLine.Option(
      names = {"--name"},
      paramLabel = "node-name",
      description = "Node name for logs identification (when several nodes running)"
  )
  private String name;

  @CommandLine.Option(
      names = {"--genesis-time"},
      paramLabel = "time",
      description = "Genesis time in GMT+0 timezone. In either form: '2019-05-24 11:23', or just"
          + " '11:23' (current day is taken). Default value is start of the current hour."
  )
  private String genesisTime;

  public String getName() {
    return name;
  }

  public Integer getListenPort() {
    return listenPort;
  }

  public List<String> getActivePeers() {
    return activePeers;
  }

  public List<String> getValidators() {
    return validators;
  }

  public String getGenesisTime() {
    return genesisTime;
  }

  @Override
  public void run() {
    NodeCommandLauncher.Builder nodeBuilder =
        new NodeCommandLauncher.Builder()
            .withLogLevel(logLevel == null ? null : logLevel.toLog4j())
            .withCliOptions(this);

    if ("default".equals(config)) {
      nodeBuilder.withConfigFromResource("/config/default-node-config.yml");
    } else {
      nodeBuilder.withConfigFromFile(new File(config));
    }

    nodeBuilder.build().run();
  }
}

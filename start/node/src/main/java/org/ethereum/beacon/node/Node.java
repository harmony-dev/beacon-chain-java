package org.ethereum.beacon.node;

import java.io.File;
import java.util.List;
import org.ethereum.beacon.node.Node.VersionProvider;
import org.ethereum.beacon.node.command.LogLevel;
import org.ethereum.beacon.start.common.ClientInfo;
import picocli.CommandLine;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.RunLast;

@CommandLine.Command(
    description = "Beacon chain node",
    name = "node",
    versionProvider = VersionProvider.class,
    mixinStandardHelpOptions = true)
public class Node implements Runnable {

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_EXIT_CODE = 1;

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "config.yml",
      description = {
          "A path to a file containing node config in YAML format.",
          "Use 'default' to run a node with default setup."
      }
  )
  private String config;

  @CommandLine.Option(
      names = {"--loglevel"},
      paramLabel = "level",
      description = {
          "Log verbosity level: all, debug, info, error.",
          "info is set by default."
      }
  )
  private LogLevel logLevel = null;

  @CommandLine.Option(
      names = {"--listen"},
      paramLabel = "port",
      description = "TCP port to listen for inbound connections."
  )
  private Integer listenPort;

  @CommandLine.Option(
      names = {"--connect"},
      paramLabel = "URL",
      split = ",",
      description = {
          "Peers that node is actively connecting to.",
          "URL format: <multiaddress>:<node id hex>",
          "URL sample: /ip4/10.0.0.128/tcp/40001:11111111111111111111111111111111111111111111111111111111111111111111"
      }
  )
  private List<String> activePeers;

  @CommandLine.Option(
      names = {"--validators"},
      paramLabel = "key",
      split = ",",
      description = {
          "Validator registry. Entry is either:",
              "  private key in a hex format prepended with '0x'",
              "  an index or a range specifying a keypair(s) in emulated deposit contract",
          "Example: --validators=1,2,5-9,0x1234567[...]ef"
      }
  )
  private List<String> validators;

  @CommandLine.Option(
      names = {"--name"},
      paramLabel = "node-name",
      description = {
          "Node identity for logs output",
          "Useful when several nodes are running"
      }
  )
  private String name;

  @CommandLine.Option(
      names = {"--genesis-time"},
      paramLabel = "time",
      description = { "Genesis time in GMT+0 timezone. In either form:",
          "  '2019-05-24 11:23'",
          "  '11:23' (current day is taken)",
          "Defaults to the beginning of the current hour." }
  )
  private String genesisTime;

  @CommandLine.Option(
      names = "--spec-constants",
      paramLabel = "spec-constants",
      description = "Path to a spec constants file in yaml format (flat format)"
  )
  private String specConstantsFile;

  @CommandLine.Option(
      names = "--metrics-endpoint",
      paramLabel = "matrics-endpoint",
      description = {
          "Interface and port, Prometheus collection endpoint will be served from.",
          "Should have form of interface:port.",
          "Default endpoint is 0.0.0.0:8008."
      }
  )
  private String metricsEndpoint;

  @CommandLine.Option(
      names = "--initial-state",
      paramLabel = "initial-state",
      description = {
          "Path to an initial state file (SSZ format)"
      }
  )
  private String initialStateFile;

  @CommandLine.Option(
      names = "--start-mode",
      paramLabel = "start-mode",
      description = {
          "Specifies how to deal with the existing or absent storage. Possible modes:",
          "  initial - starts from an empty storage only. If it's not, --force-db-clean can be specified.",
          "  storage - starts from a previously initialized storage only,",
          "            ignoring contract/initial-state parameters",
          "  auto    - starts from an existing storage, if it's non empty",
          "            initializes from contract/initial-state parameters otherwise",
          "By default, set to auto, if no initial-state is specified,",
          "            and to initial, if an initial-state is specified."
      }
  )
  private String startMode;

  @CommandLine.Option(
      names = "--force-db-clean",
      paramLabel = "force-db-clean",
      description = {
          "When an initial-state is specified, but db is not empty",
          "specifies how to resolve the problem:",
          "  force-db-clean=true  - tries to clean db",
          "  force-db-clean=false - exits with failure status.",
          "False by default."
      }
  )
  private boolean forceDBClean = false;

  @CommandLine.Option(
      names = "--db-prefix",
      paramLabel = "db-prefix",
      description = "Specifies db-prefix, used to construct db directory"
  )
  private String dbPrefix;

  @CommandLine.Option(
      names = {"--initial-deposit-count", "--validator-count"},
      paramLabel = "initial-deposit-count",
      description = {
          "Specifies amount of initial deposits when constructing a genesis state."
      }
  )
  private Integer initialDepositCount;

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

  public String getSpecConstantsFile() {
    return specConstantsFile;
  }

  public String getMetricsEndpoint() {
    return metricsEndpoint;
  }

  public String getInitialStateFile() {
    return initialStateFile;
  }

  public boolean isForceDBClean() {
    return forceDBClean;
  }

  public String getStartMode() {
    return startMode;
  }

  public String getDbPrefix() {
    return dbPrefix;
  }

  public Integer getInitialDepositCount() {
    return initialDepositCount;
  }

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

  static class VersionProvider implements IVersionProvider {
    @Override
    public String[] getVersion() throws Exception {
      return new String[] {ClientInfo.fullTitleVersion(Node.class)};
    }
  }
}

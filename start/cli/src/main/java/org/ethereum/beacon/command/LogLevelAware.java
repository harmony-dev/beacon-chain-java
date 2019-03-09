package org.ethereum.beacon.command;

import org.apache.logging.log4j.Level;
import picocli.CommandLine;

public abstract class LogLevelAware {

  @CommandLine.Option(
      order = Integer.MAX_VALUE,
      names = {"--loglevel"},
      paramLabel = "level",
      description = "Log verbosity level: all, debug, info, error\ninfo is set by default")
  protected Level logLevel;
}

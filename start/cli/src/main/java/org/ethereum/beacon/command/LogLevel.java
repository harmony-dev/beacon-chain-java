package org.ethereum.beacon.command;

import org.apache.logging.log4j.Level;

public enum LogLevel {
  all,
  debug,
  info,
  error;

  public Level toLog4j() {
    switch (this) {
      case all:
        return Level.ALL;
      case debug:
        return Level.DEBUG;
      case info:
        return Level.INFO;
      case error:
        return Level.ERROR;
      default:
        throw new IllegalArgumentException("Unsupported log level " + this);
    }
  }
}

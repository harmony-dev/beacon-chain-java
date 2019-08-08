package org.ethereum.beacon.validator.api;

/** Set of endpoints to enable a working validator server */
public interface ValidatorServer {
  void start();
  void stop();
}

package org.ethereum.beacon.wire.exceptions;

/**
 * Is thrown on malformed request from remote side
 */
public class WireIllegalArgumentsException extends WireException {

  public WireIllegalArgumentsException(String message) {
    super(message);
  }
}

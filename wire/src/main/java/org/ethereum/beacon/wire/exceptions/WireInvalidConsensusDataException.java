package org.ethereum.beacon.wire.exceptions;

public class WireInvalidConsensusDataException extends WireException {

  public WireInvalidConsensusDataException(String message) {
    super(message);
  }

  public WireInvalidConsensusDataException(String message, Throwable cause) {
    super(message, cause);
  }
}

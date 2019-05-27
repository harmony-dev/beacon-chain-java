package org.ethereum.beacon.wire.exceptions;

/**
 * Is thrown when the data from a remote party violates consensus rules
 */
public class WireInvalidConsensusDataException extends WireException {

  public WireInvalidConsensusDataException(String message) {
    super(message);
  }

  public WireInvalidConsensusDataException(String message, Throwable cause) {
    super(message, cause);
  }
}

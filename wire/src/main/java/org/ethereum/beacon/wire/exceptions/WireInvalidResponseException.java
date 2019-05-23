package org.ethereum.beacon.wire.exceptions;

/**
 * When a remote party replied with invalid or inconsistent data to our request
 */
public class WireInvalidResponseException extends WireException {

  public WireInvalidResponseException(String message) {
    super(message);
  }
}

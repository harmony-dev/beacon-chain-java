package org.ethereum.beacon.wire.exceptions;

public class WireRpcClosedException extends WireRpcException {

  public WireRpcClosedException(String message) {
    super(message);
  }

  public WireRpcClosedException(String message, Throwable cause) {
    super(message, cause);
  }
}

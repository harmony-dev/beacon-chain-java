package org.ethereum.beacon.wire.exceptions;

public class WireRpcException extends WireException {

  public WireRpcException(String message) {
    super(message);
  }

  public WireRpcException(String message, Throwable cause) {
    super(message, cause);
  }
}

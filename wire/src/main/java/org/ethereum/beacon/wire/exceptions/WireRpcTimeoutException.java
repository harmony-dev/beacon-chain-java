package org.ethereum.beacon.wire.exceptions;

public class WireRpcTimeoutException extends WireRpcException {

  public WireRpcTimeoutException(String message) {
    super(message);
  }

  public WireRpcTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}

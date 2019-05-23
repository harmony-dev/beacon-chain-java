package org.ethereum.beacon.wire.exceptions;

/**
 * Thrown when no reply to RPC request for some time
 */
public class WireRpcTimeoutException extends WireRpcException {

  public WireRpcTimeoutException(String message) {
    super(message);
  }

  public WireRpcTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}

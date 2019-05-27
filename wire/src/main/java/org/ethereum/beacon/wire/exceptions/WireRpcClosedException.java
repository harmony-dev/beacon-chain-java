package org.ethereum.beacon.wire.exceptions;

/**
 * Thrown when calling RPC method on closed channel
 */
public class WireRpcClosedException extends WireRpcException {

  public WireRpcClosedException(String message) {
    super(message);
  }

  public WireRpcClosedException(String message, Throwable cause) {
    super(message, cause);
  }
}

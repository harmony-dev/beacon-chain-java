package org.ethereum.beacon.wire.exceptions;

/**
 * This exception is a 'deserialized version' of error answer from remote RPC party
 */
public class WireRpcRemoteError extends WireRpcException {

  public WireRpcRemoteError(String message) {
    super(message);
  }
}

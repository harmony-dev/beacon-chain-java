package org.ethereum.beacon.wire.exceptions;

/**
 * This exception is a 'deserialized version' of error answer from remote RPC party
 */
public class WireRemoteRpcError extends WireRpcException {

  public WireRemoteRpcError(String message) {
    super(message);
  }
}

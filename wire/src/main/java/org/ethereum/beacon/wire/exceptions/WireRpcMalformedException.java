package org.ethereum.beacon.wire.exceptions;

/**
 * Indicates remote side RPC protocol violation
 */
public class WireRpcMalformedException extends WireRpcException {

  public WireRpcMalformedException(String message) {
    super(message);
  }
}

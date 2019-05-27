package org.ethereum.beacon.wire.message;

public abstract class RequestMessagePayload extends MessagePayload {

  public abstract int getMethodId();
}

package org.ethereum.beacon.wire.message;

public abstract class ResponseMessagePayload
    extends MessagePayload {

  private final Object request;

  public ResponseMessagePayload(Object request) {
    this.request = request;
  }

  public Object getRequest() {
    return request;
  }
}

package org.ethereum.beacon.wire.message;

public abstract class ResponseMessagePayload<RequestType extends RequestMessagePayload>
    extends MessagePayload {

  private final RequestType request;

  public ResponseMessagePayload(RequestType request) {
    this.request = request;
  }

  public RequestType getRequest() {
    return request;
  }
}

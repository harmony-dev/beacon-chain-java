package org.ethereum.beacon.wire.impl.plain.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RpcMessage<TRequest, TResponse> {
  private final TRequest request;
  private final boolean isNotification;
  private final TResponse response;
  private final Throwable error;
  private final Map<Object, Object> requestContext = new HashMap<>();

  public RpcMessage(TRequest request, boolean isNotification) {
    this(request, isNotification, null, null);
  }

  public RpcMessage(TRequest request, TResponse response) {
    this(request, false, response, null);
  }

  public RpcMessage(TRequest request, Throwable error) {
    this(request, false, null, error);
  }

  private RpcMessage(TRequest request, boolean isNotification, TResponse response, Throwable error) {
    this.request = request;
    this.isNotification = isNotification;
    this.response = response;
    this.error = error;
  }

  public TRequest getRequest() {
    return request;
  }

  public boolean isNotification() {
    return isNotification;
  }

  public Optional<TResponse> getResponse() {
    return Optional.ofNullable(response);
  }

  public Optional<Throwable> getError() {
    return Optional.ofNullable(error);
  }

  public boolean isRequest() {
    return !(getResponse().isPresent() || getError().isPresent());
  }

  public boolean isResponse() {
    return !isRequest();
  }

  public <TNewRequest, TNewResponse> RpcMessage<TNewRequest, TNewResponse> copyWithRequest(TNewRequest newRequest) {
    if (!isRequest()) {
      throw new IllegalStateException("");
    }
    RpcMessage<TNewRequest, TNewResponse> ret = new RpcMessage<>(newRequest,
        isNotification, null, null);
    ret.requestContext.putAll(requestContext);
    return ret;
  }

  public <TNewRequest, TNewResponse> RpcMessage<TNewRequest, TNewResponse> copyWithResponse(TNewResponse newResponse) {
    if (getError().isPresent()) {
      throw new IllegalStateException("");
    }
    RpcMessage<TNewRequest, TNewResponse> ret = new RpcMessage<>(null,
        false, newResponse, null);
    ret.requestContext.putAll(requestContext);
    return ret;
  }

  public <TNewRequest, TNewResponse> RpcMessage<TNewRequest, TNewResponse> copyWithResponseError(Throwable error) {
    if (getError().isPresent()) {
      throw new IllegalStateException("");
    }
    RpcMessage<TNewRequest, TNewResponse> ret = new RpcMessage<>(null,
        false, null, error);
    ret.requestContext.putAll(requestContext);
    return ret;
  }

  public void setRequestContext(Object key, Object value) {
    if (!isRequest()) {
      throw new IllegalStateException("Context can be added to request only");
    }
    requestContext.put(key, value);
  }

  public Object getRequestContext(Object key) {
    if (!isResponse()) {
      throw new IllegalStateException("Context can be pushed from response only");
    }
    return requestContext.get(key);
  }

  Map<Object, Object> getRequestContext() {
    return requestContext;
  }
}

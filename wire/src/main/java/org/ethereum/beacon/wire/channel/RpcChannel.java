package org.ethereum.beacon.wire.channel;

public interface RpcChannel<TRequest, TResponse> extends Channel<RpcMessage<TRequest, TResponse>> {

  static <TRequest, TResponse> RpcChannel<TRequest, TResponse> from(Channel<RpcMessage<TRequest, TResponse>> channel) {
    return (RpcChannel<TRequest, TResponse>) channel;
  }
}

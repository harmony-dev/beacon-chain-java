package org.ethereum.beacon.wire.channel;

import org.reactivestreams.Publisher;

public interface RpcChannel<TRequest, TResponse> extends Channel<RpcMessage<TRequest, TResponse>> {

  static <TRequest, TResponse> RpcChannel<TRequest, TResponse> from(Channel<RpcMessage<TRequest, TResponse>> channel) {
    return new RpcChannel<TRequest, TResponse>() {
      @Override
      public Publisher<RpcMessage<TRequest, TResponse>> inboundMessageStream() {
        return channel.inboundMessageStream();
      }

      @Override
      public void subscribeToOutbound(
          Publisher<RpcMessage<TRequest, TResponse>> outboundMessageStream) {
        channel.subscribeToOutbound(outboundMessageStream);
      }
    } ;
  }
}

package org.ethereum.beacon.wire.channel;

import java.util.function.Predicate;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class RpcChannelClassFilter<TInReq, TInResp, TRequest extends TInReq, TResponse extends TInResp>
    implements ChannelOp<RpcMessage<TInReq, TInResp>, RpcMessage<TRequest, TResponse>> {
  private static final Object CONTEXT_KEY_REQ_CLASS = new Object();

  private final RpcChannel<TInReq, TInResp> inChannel;
  private final Class<TRequest> requestMessageClass;

  public RpcChannelClassFilter(RpcChannel<TInReq, TInResp> inChannel, Class<TRequest> requestMessageClass) {
    this.inChannel = inChannel;
    this.requestMessageClass = requestMessageClass;
  }

  @Override
  public Publisher<RpcMessage<TRequest, TResponse>> inboundMessageStream() {
    return Flux.from(inChannel.inboundMessageStream())
        .filter(rpcMsg -> rpcMsg.isRequest() && requestMessageClass.isInstance(rpcMsg.getRequest()))
        .filter(rpcMsg -> rpcMsg.isResponse() && requestMessageClass == rpcMsg.popRequestContext(CONTEXT_KEY_REQ_CLASS))
        .map(msg -> (RpcMessage<TRequest, TResponse>) msg);
  }

  @Override
  public void subscribeToOutbound(Publisher<RpcMessage<TRequest, TResponse>> outboundMessageStream) {
    inChannel.subscribeToOutbound(Flux.from(outboundMessageStream)
        .doOnNext(rpcMsg -> {
          if (rpcMsg.isRequest()) {
            if (!requestMessageClass.isInstance(rpcMsg.getRequest())) {
              throw new IllegalArgumentException("Invalid request class: " + rpcMsg.getRequest().getClass() + ", expected " + requestMessageClass);
            }
            rpcMsg.pushRequestContext(CONTEXT_KEY_REQ_CLASS, requestMessageClass);
          }
        })
        .map(msg -> (RpcMessage<TInReq, TInResp>) msg)
    );
  }

  @Override
  public Channel<RpcMessage<TInReq, TInResp>> getInChannel() {
    return inChannel;
  }
}

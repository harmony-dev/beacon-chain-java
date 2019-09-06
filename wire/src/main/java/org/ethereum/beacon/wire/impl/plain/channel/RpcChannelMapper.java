package org.ethereum.beacon.wire.impl.plain.channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.ethereum.beacon.wire.exceptions.WireException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public abstract class RpcChannelMapper<TInMessage, TOutRequest, TOutResponse>
    implements RpcChannel<TOutRequest, TOutResponse>,
        ChannelOp<TInMessage, RpcMessage<TOutRequest, TOutResponse>> {

  private static final Object CONTEXT_ID_KEY = new Object();

  private final Map<Object, Map<Object, Object>> idToContextMap = new ConcurrentHashMap<>();
  private final Channel<TInMessage> inChannel;

  protected RpcChannelMapper(Channel<TInMessage> inChannel) {
    this.inChannel = inChannel;
  }

  @Override
  public Publisher<RpcMessage<TOutRequest, TOutResponse>> inboundMessageStream() {
    return Flux.from(inChannel.inboundMessageStream()).map(this::fromIn);
  }

  protected RpcMessage<TOutRequest, TOutResponse> fromIn(TInMessage msg) {
    if (isRequest(msg)) {
      if (isNotification(msg)) {
        return new RpcMessage<TOutRequest, TOutResponse>((TOutRequest) msg, true);
      } else {
        RpcMessage<TOutRequest, TOutResponse> rpcMessage = new RpcMessage<>(
            (TOutRequest) msg, false);
        rpcMessage.setRequestContext(CONTEXT_ID_KEY, getId(msg));
        return rpcMessage;
      }
    } else {
      Object id = getId(msg);
      Map<Object, Object> requestContext = idToContextMap.remove(id);
      if (requestContext == null) {
        throw new WireException("Invalid response from remote: can't find request ID: " + id + ", " + msg);
      }
      RpcMessage<TOutRequest, TOutResponse> rpcMessage = new RpcMessage<>(null, (TOutResponse) msg);
      rpcMessage.getRequestContext().putAll(requestContext);
      return rpcMessage;
    }
  }

  protected TInMessage toIn(RpcMessage<TOutRequest, TOutResponse> msg) {
    if (msg.isRequest()) {
      TInMessage inMessage = (TInMessage) msg.getRequest();
      Object id = generateNextId();
      setId(inMessage, id);
      if (msg.isNotification()) {
        return (TInMessage) msg.getRequest();
      } else {
        idToContextMap.put(id, msg.getRequestContext());
        return inMessage;
      }
    } else {
      TInMessage inMessage = (TInMessage) msg.getResponse().get();
      setId(inMessage, msg.getRequestContext(CONTEXT_ID_KEY));
      return inMessage;
    }
  }

    @Override
  public void subscribeToOutbound(Publisher<RpcMessage<TOutRequest, TOutResponse>> outboundMessageStream) {
    inChannel.subscribeToOutbound(Flux.from(outboundMessageStream).map(this::toIn));
  }

  public Channel<TInMessage> getInChannel() {
    return inChannel;
  }

  protected abstract boolean isRequest(TInMessage msg);

  protected abstract boolean isNotification(TInMessage msg);

  protected abstract Object generateNextId();

  protected abstract Object getId(TInMessage msg);

  protected abstract void setId(TInMessage msg, Object id);

}

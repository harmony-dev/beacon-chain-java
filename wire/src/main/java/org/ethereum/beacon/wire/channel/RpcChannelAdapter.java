package org.ethereum.beacon.wire.channel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.ethereum.beacon.wire.exceptions.WireException;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

public class RpcChannelAdapter<TRequestMessage, TResponseMessage> {

  private static final Object CONTEXT_KEY_FUTURE = new Object();

  private final RpcChannel<TRequestMessage, TResponseMessage> inChannel;
  private final Function<TRequestMessage, CompletableFuture<TResponseMessage>> serverHandler;
  private final DirectProcessor<RpcMessage<TRequestMessage, TResponseMessage>> outboundStream =
      DirectProcessor.create();


  public RpcChannelAdapter(RpcChannel<TRequestMessage, TResponseMessage> inChannel,
      Function<TRequestMessage, CompletableFuture<TResponseMessage>> serverHandler) {
    this.inChannel = inChannel;
    this.serverHandler = serverHandler;
    inChannel.subscribeToOutbound(outboundStream);
    Flux.from(inChannel.inboundMessageStream()).subscribe(this::onInbound);
  }

  private void onInbound(RpcMessage<TRequestMessage, TResponseMessage> msg) {
    if (msg.isRequest()) {
      if (msg.isNotification()) {
        handleNotify(msg.getRequest());
      } else {
        handleInvoke(msg);
      }
    } else {
      handleResponse(msg);
    }
  }

  private void handleResponse(RpcMessage<TRequestMessage, TResponseMessage> msg) {
    CompletableFuture<TResponseMessage> respFut =
        (CompletableFuture<TResponseMessage>) msg.getRequestContext(CONTEXT_KEY_FUTURE);

    if (msg.getResponse().isPresent()) {
      respFut.complete(msg.getResponse().get());
    } else {
      respFut.completeExceptionally(msg.getError().get());
    }
  }

  private void handleNotify(TRequestMessage msg) {
    if (serverHandler != null) {
      serverHandler.apply(msg);
    }
  }

  private void handleInvoke(RpcMessage<TRequestMessage, TResponseMessage> msg) {
    try {
      if (serverHandler == null) {
        throw new WireException("No server to process RPC invoke: " + msg);
      }
      CompletableFuture<TResponseMessage> fut = serverHandler.apply(msg.getRequest());
      fut.whenComplete(
              (r, t) ->
                  outboundStream.onNext(
                      t != null ? msg.copyWithResponseError(t) : msg.copyWithResponse(r)))
          .whenComplete(
              (r, t) -> {
                if (t != null) t.printStackTrace();
              });
    } catch (Exception e) {
      outboundStream.onNext(msg.copyWithResponseError(e));
    }
  }

  public CompletableFuture<TResponseMessage> invokeRemote(TRequestMessage request) {
    RpcMessage<TRequestMessage, TResponseMessage> requestRpcMsg =
        new RpcMessage<>(request, false);
    CompletableFuture<TResponseMessage> ret = new CompletableFuture<>();
    requestRpcMsg.setRequestContext(CONTEXT_KEY_FUTURE, ret);
    outboundStream.onNext(requestRpcMsg);
    return ret;
  }

  public void notifyRemote(TRequestMessage request) {
    outboundStream.onNext(new RpcMessage<>(request, true));
  }
}

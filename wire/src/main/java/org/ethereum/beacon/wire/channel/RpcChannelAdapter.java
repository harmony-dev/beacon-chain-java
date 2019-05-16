package org.ethereum.beacon.wire.channel;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.wire.exceptions.WireException;
import org.ethereum.beacon.wire.exceptions.WireRpcClosedException;
import org.ethereum.beacon.wire.exceptions.WireRpcTimeoutException;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

public class RpcChannelAdapter<TRequestMessage, TResponseMessage> {
  private static final Logger logger = LogManager.getLogger(RpcChannelAdapter.class);

  private static final Object CONTEXT_KEY_FUTURE = new Object();
  public static final Duration DEFAULT_RPC_TIMEOUT = Duration.ofSeconds(10);

  private final Scheduler timeoutScheduler;
  private Duration rpcCallTimeout = DEFAULT_RPC_TIMEOUT;
  private final RpcChannel<TRequestMessage, TResponseMessage> inChannel;
  private final Function<TRequestMessage, CompletableFuture<TResponseMessage>> serverHandler;
  private FluxSink<RpcMessage<TRequestMessage, TResponseMessage>> outboundSink;
  private volatile boolean closed;


  public RpcChannelAdapter(RpcChannel<TRequestMessage, TResponseMessage> inChannel,
      Function<TRequestMessage, CompletableFuture<TResponseMessage>> serverHandler,
      Scheduler timeoutScheduler) {
    this.inChannel = inChannel;
    this.serverHandler = serverHandler;
    this.timeoutScheduler = timeoutScheduler;
    inChannel.subscribeToOutbound(Flux.create(s -> outboundSink = s));
    Flux.from(inChannel.inboundMessageStream())
        .subscribe(this::onInbound, err -> logger.warn("Unexpected error", err), this::onClose);
  }

  public RpcChannelAdapter<TRequestMessage, TResponseMessage> withRpcCallTimeout(
      Duration rpcCallTimeout) {
    this.rpcCallTimeout = rpcCallTimeout;
    return this;
  }

  private void onClose() {
    closed = true;
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
                  outboundSink.next(
                      t != null ? msg.copyWithResponseError(t) : msg.copyWithResponse(r)))
          .whenComplete(
              (r, t) -> {
                if (t != null) t.printStackTrace();
              });
    } catch (Exception e) {
      outboundSink.next(msg.copyWithResponseError(e));
    }
  }

  public CompletableFuture<TResponseMessage> invokeRemote(TRequestMessage request) {
    CompletableFuture<TResponseMessage> ret = new CompletableFuture<>();

    if(closed) {
      ret.completeExceptionally(new WireRpcClosedException("Channel already closed: " + inChannel));
      return ret;
    } else {
      RpcMessage<TRequestMessage, TResponseMessage> requestRpcMsg =
          new RpcMessage<>(request, false);
      requestRpcMsg.setRequestContext(CONTEXT_KEY_FUTURE, ret);
      outboundSink.next(requestRpcMsg);
      return timeoutScheduler.orTimeout(
          ret,
          rpcCallTimeout,
          () -> closed
                  ? new WireRpcClosedException("Channel was closed during call execution")
                  : new WireRpcTimeoutException("RPC call timeout."));
    }
  }

  public void notifyRemote(TRequestMessage request) {
    outboundSink.next(new RpcMessage<>(request, true));
  }
}

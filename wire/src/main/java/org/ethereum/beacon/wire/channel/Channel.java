package org.ethereum.beacon.wire.channel;

import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface Channel<TMessage> {

  Publisher<TMessage> inboundMessageStream();

  void subscribeToOutbound(Publisher<TMessage> outboundMessageStream);

  default CompletableFuture<Void> getCloseFuture() {
    return Mono.ignoreElements(inboundMessageStream()).toFuture().thenApply(ignore -> null);
  }

  default void close() {
  }
}

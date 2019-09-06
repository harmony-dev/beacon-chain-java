package org.ethereum.beacon.wire.impl.plain.channel;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.wire.PeerConnection;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Duplex channel handling one (inbound) or two (inbound/outbound) streams of abstract messages
 * The channel is assumed closed when {@link #inboundMessageStream()} is in <code>Complete</code>
 * state.
 */
public interface Channel<TMessage> extends PeerConnection {

  /**
   * Returns the steam of inbound messages. When the stream completes the channel is assumed
   * closed.
   * The publisher returned must cache all messages and flush them upon the first subscription.
   * It may handle several subscribers and it's upon implementation what past messages
   * to replay to later subscribers
   */
  Publisher<TMessage> inboundMessageStream();

  /**
   * This method is called if the client wants to send any messages to this channel.
   * Normally the outboundMessageStream is immediately subscribed to during the call
   * When this method called more than than once the behaviour is not specified
   * but advanced implementations may merge messages from different publishers
   */
  void subscribeToOutbound(Publisher<TMessage> outboundMessageStream);

  /**
   * Returns the future which completes when this channel is closed.
   * This default implementation just subscribes to {@link #inboundMessageStream()} and
   * waits for it to complete. Implementing classes may override it for more effective implementation.
   */
  default CompletableFuture<Void> getCloseFuture() {
    return Mono.ignoreElements(inboundMessageStream()).toFuture().thenApply(ignore -> null);
  }

  /**
   * Closes this channel. {@link #inboundMessageStream()} will complete
   * synchronously/asynchronously during/after this call.
   */
  default void close() {
  }
}

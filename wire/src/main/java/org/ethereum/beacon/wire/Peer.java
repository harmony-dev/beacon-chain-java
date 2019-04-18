package org.ethereum.beacon.wire;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.wire.message.GoodbyeMessage;
import org.ethereum.beacon.wire.message.Message;
import org.reactivestreams.Publisher;

public interface Peer {

  Publisher<Message> getInboundMessageStream();

  void sendMessage(Message message);

  boolean isRemoteInitiated();

  CompletableFuture<GoodbyeMessage> getDisconnectFuture();

  void disconnect();

  default boolean isConnected() {
    return !getDisconnectFuture().isDone();
  }
}

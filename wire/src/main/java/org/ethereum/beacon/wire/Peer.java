package org.ethereum.beacon.wire;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.wire.impl.plain.channel.Channel;
import org.ethereum.beacon.wire.message.payload.HelloMessage;

/**
 * Represent connected peer
 */
public interface Peer {

  CompletableFuture<HelloMessage> getRemoteHelloMessage();

  /**
   * Returns raw bytes {@link Channel} of this peer
   */
  PeerConnection getConnection();

  /**
   * Returns {@link WireApiSync} instance linked to this peer
   */
  WireApiSync getSyncApi();

  /**
   * Returns {@link WireApiSub} instance linked to this peer
   */
  WireApiSub getSubApi();

}

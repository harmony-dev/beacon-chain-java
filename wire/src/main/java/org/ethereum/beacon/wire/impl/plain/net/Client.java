package org.ethereum.beacon.wire.impl.plain.net;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.wire.impl.plain.channel.Channel;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * An abstract client which can connect to remote party by supplying its abstract TAddress
 */
public interface Client<TAddress> {

  /**
   * Connects to remote party and returns the bytes {@link Channel} upon connection
   * If connecting fails the {@link CompletableFuture} return will be completed with exception
   */
  <C extends Channel<BytesValue>> CompletableFuture<C> connect(TAddress address);

}

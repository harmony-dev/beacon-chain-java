package org.ethereum.beacon.wire.net;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.wire.channel.Channel;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface Client<TAddress> {

  <C extends Channel<BytesValue>> CompletableFuture<C> connect(TAddress address);

}

package org.ethereum.beacon.wire.net;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.wire.channel.Channel;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface Client<TAddress> {

  CompletableFuture<? extends Channel<BytesValue>> connect(TAddress address);

}

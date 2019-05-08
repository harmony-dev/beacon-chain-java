package org.ethereum.beacon.wire;

import org.ethereum.beacon.wire.channel.Channel;
import org.ethereum.beacon.wire.message.Message;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface Peer {

  Channel<BytesValue> getRawChannel();

  boolean isRemoteInitiated();

}

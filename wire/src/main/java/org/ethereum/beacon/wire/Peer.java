package org.ethereum.beacon.wire;

import org.ethereum.beacon.wire.channel.Channel;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface Peer {

  Channel<BytesValue> getRawChannel();

  WireApiSync getSyncApi();

  WireApiSub getSubApi();

}

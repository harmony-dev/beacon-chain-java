package org.ethereum.beacon.wire;

import org.ethereum.beacon.wire.channel.Channel;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Represent connected peer
 */
public interface Peer {

  /**
   * Returns raw bytes {@link Channel} of this peer
   */
  Channel<BytesValue> getRawChannel();

  /**
   * Returns {@link WireApiSync} instance linked to this peer
   */
  WireApiSync getSyncApi();

  /**
   * Returns {@link WireApiSub} instance linked to this peer
   */
  WireApiSub getSubApi();

}

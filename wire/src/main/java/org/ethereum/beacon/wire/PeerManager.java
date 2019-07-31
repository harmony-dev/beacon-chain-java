package org.ethereum.beacon.wire;

import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.stream.RxUtil;
import org.reactivestreams.Publisher;

import java.util.List;

/** Manages connected peers and aggregates their `high-level` APIs */
public interface PeerManager {

  /** Stream of new peer connections */
  Publisher<Peer> connectedPeerStream();

  /**
   * Stream of peer disconnects Peer must occur in this stream strictly after occurring in the
   * {@link #connectedPeerStream()}
   */
  Publisher<Peer> disconnectedPeerStream();

  /**
   * Steam of new active peers which are connected and handshake was done. A peer appearing in this
   * stream is available for 'high-level' APIs calls
   */
  Publisher<Peer> activatedPeerStream();

  /** Stream of currently active peers list */
  default Publisher<List<Peer>> activePeersStream() {
    return RxUtil.collect(activatedPeerStream(), disconnectedPeerStream());
  }

  /**
   * Returns WireApiSync instance which is the aggregation of all connected peer APIs When currently
   * no active peers the API enqueues invocations and execute them upon any active peer connected
   */
  WireApiSync getWireApiSync();

  /**
   * Returns WireApiSub instance which is the aggregation of all connected peer APIs. When currently
   * no active peers the instance just ignores outbound notifications.
   */
  WireApiSub getWireApiSub();

  /** Returns best known slot among all peers ever connected */
  Publisher<SlotNumber> getMaxSlotStream();
}

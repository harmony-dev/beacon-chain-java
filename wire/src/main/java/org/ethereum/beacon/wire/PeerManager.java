package org.ethereum.beacon.wire;

import java.util.Collection;
import org.reactivestreams.Publisher;

public interface PeerManager {

  Publisher<Peer> connectedPeerStream();

  Publisher<Peer> disconnectedPeerStream();

  Publisher<Peer> activePeerStream();

  Collection<Peer> getActivePeers();

  WireApiSync getWireApiSync();

  WireApiSub getWireApiSub();
}

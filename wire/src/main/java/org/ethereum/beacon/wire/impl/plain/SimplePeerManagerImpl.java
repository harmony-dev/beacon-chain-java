package org.ethereum.beacon.wire.impl.plain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.MessageSerializer;
import org.ethereum.beacon.wire.Peer;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.impl.AbstractPeerManager;
import org.ethereum.beacon.wire.impl.plain.channel.Channel;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SimplePeerManagerImpl extends AbstractPeerManager {
  private static final Logger logger = LogManager.getLogger(SimplePeerManagerImpl.class);

  private final SSZSerializer ssz;
  private final MessageSerializer messageSerializer;
  private final WireApiSync syncServer;
  private final WireApiSubRouter wireApiSubRouter;

  public SimplePeerManagerImpl(
      Publisher<Channel<BytesValue>> channelsStream,
      SSZSerializer ssz,
      BeaconChainSpec spec,
      MessageSerializer messageSerializer,
      Schedulers schedulers,
      WireApiSync syncServer,
      Publisher<BeaconTupleDetails> headStream) {
    super(spec, Bytes4.ZERO, schedulers, headStream);

    this.ssz = ssz;
    this.messageSerializer = messageSerializer;
    this.syncServer = syncServer;

    Flux.from(channelsStream).subscribe(ch -> onNewPeer(createPeer(ch)));

    wireApiSubRouter = new WireApiSubRouter(
        Flux.from(activatedPeerStream()).map(Peer::getSubApi),
        Flux.from(disconnectedPeerStream()).map(Peer::getSubApi));
  }

  protected PeerImpl createPeer(Channel<BytesValue> channel) {
    logger.info("Creating a peer from new channel: " + channel);
    return new PeerImpl(channel, createLocalHello(), ssz, messageSerializer, syncServer, schedulers);
  }

  @Override
  public WireApiSub getWireApiSub() {
    return wireApiSubRouter;
  }
}

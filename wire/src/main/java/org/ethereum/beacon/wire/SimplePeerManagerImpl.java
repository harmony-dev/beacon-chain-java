package org.ethereum.beacon.wire;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.channel.Channel;
import org.ethereum.beacon.wire.message.payload.HelloMessage;
import org.ethereum.beacon.wire.sync.WireApiSyncRouter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class SimplePeerManagerImpl implements PeerManager {
  private static final Logger logger = LogManager.getLogger(SimplePeerManagerImpl.class);

  private final byte networkId;
  private final UInt64 chainId;

  private final Publisher<Channel<BytesValue>> channelsStream;
  private final SSZSerializer ssz;
  private final BeaconChainSpec spec;
  private final MessageSerializer messageSerializer;
  private final Schedulers schedulers;
  private final WireApiSync syncServer;
  private final Publisher<BeaconTupleDetails> headStream;
  private final WireApiSyncRouter wireApiSyncRouter;
  private final WireApiSubRouter wireApiSubRouter;


  private final Flux<PeerImpl> connectedPeersStream;
  private final List<Peer> activePeers = Collections.synchronizedList(new ArrayList<>());

  public SimplePeerManagerImpl(
      byte networkId,
      UInt64 chainId,
      Publisher<Channel<BytesValue>> channelsStream,
      SSZSerializer ssz,
      BeaconChainSpec spec,
      MessageSerializer messageSerializer,
      Schedulers schedulers,
      WireApiSync syncServer,
      Publisher<BeaconTupleDetails> headStream) {

    this.networkId = networkId;
    this.chainId = chainId;
    this.channelsStream = channelsStream;
    this.ssz = ssz;
    this.spec = spec;
    this.messageSerializer = messageSerializer;
    this.schedulers = schedulers;
    this.syncServer = syncServer;
    this.headStream = headStream;

    connectedPeersStream = Flux.from(channelsStream)
        .map(this::createPeer)
        .replay(1).autoConnect();

    Flux.from(activePeerStream()).subscribe(this::onNewActivePeer);

    wireApiSyncRouter = new WireApiSyncRouter(
        Flux.from(activePeerStream()).map(Peer::getSyncApi),
        Flux.from(disconnectedPeerStream()).map(Peer::getSyncApi));

    wireApiSubRouter = new WireApiSubRouter(
        Flux.from(activePeerStream()).map(Peer::getSubApi),
        Flux.from(disconnectedPeerStream()).map(Peer::getSubApi));
  }

   protected HelloMessage createLocalHello() {
    BeaconTupleDetails head = Mono.from(headStream).block(Duration.ofSeconds(10)); // TODO
    return new HelloMessage(
        networkId,
        chainId,
        head.getFinalState().getFinalizedRoot(),
        head.getFinalState().getFinalizedEpoch(),
        spec.getObjectHasher().getHashTruncateLast(head.getBlock()),
        head.getBlock().getSlot());
  }

  protected PeerImpl createPeer(Channel<BytesValue> channel) {
    logger.info("Creating a peer from new channel: " + channel);
    return new PeerImpl(channel, createLocalHello(), ssz, messageSerializer, syncServer, schedulers);
  }

  @Override
  public Publisher<Peer> connectedPeerStream() {
    return connectedPeersStream.map(p -> p);
  }

  @Override
  public Publisher<Peer> disconnectedPeerStream() {
    return connectedPeersStream.flatMap(
        peer -> Mono.fromFuture(peer.getRawChannel().getCloseFuture().thenApply(v -> peer)));
  }

  @Override
  public Publisher<Peer> activePeerStream() {
    return connectedPeersStream.flatMap(
        peer -> Mono.fromFuture(peer.getPeerActiveFuture().thenApply(v -> peer)));
  }

  protected void onNewActivePeer(Peer peer) {
    logger.info("New active peer: " + peer);
    activePeers.add(peer);
    peer.getRawChannel().getCloseFuture().thenAccept(v -> activePeers.remove(peer));
  }

  @Override
  public Collection<Peer> getActivePeers() {
    return activePeers;
  }

  @Override
  public WireApiSync getWireApiSync() {
    return wireApiSyncRouter;
  }

  @Override
  public WireApiSub getWireApiSub() {
    return wireApiSubRouter;
  }
}

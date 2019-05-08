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
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.channel.Channel;
import org.ethereum.beacon.wire.message.payload.HelloMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class SimplePeerManagerImpl implements PeerManager {
  private static final Logger logger = LogManager.getLogger(SimplePeerManagerImpl.class);

  private byte networkId;
  private UInt64 chainId;

  Publisher<Channel<BytesValue>> channelsStream;
  SSZSerializer ssz;
  BeaconChainSpec spec;
  MessageSerializer messageSerializer;
  WireApiSync syncServer;
  Publisher<BeaconTupleDetails> headStream;

  Flux<PeerImpl> connectedPeersStream;
  List<Peer> activePeers = Collections.synchronizedList(new ArrayList<>());

  public SimplePeerManagerImpl(
      byte networkId,
      UInt64 chainId,
      Publisher<Channel<BytesValue>> channelsStream,
      SSZSerializer ssz,
      BeaconChainSpec spec,
      MessageSerializer messageSerializer,
      WireApiSync syncServer,
      Publisher<BeaconTupleDetails> headStream) {

    this.networkId = networkId;
    this.chainId = chainId;
    this.channelsStream = channelsStream;
    this.ssz = ssz;
    this.spec = spec;
    this.messageSerializer = messageSerializer;
    this.syncServer = syncServer;
    this.headStream = headStream;

    connectedPeersStream = Flux.from(channelsStream).map(this::createPeer).publish().autoConnect();

    Flux.from(activePeerStream()).subscribe(this::onNewActivePeer);
  }

   protected HelloMessage createLocalHello() {
    BeaconTupleDetails head = Flux.from(headStream).last().block(Duration.ZERO);
    return new HelloMessage(
        networkId,
        chainId,
        head.getFinalState().getFinalizedRoot(),
        head.getFinalState().getFinalizedEpoch(),
        spec.getObjectHasher().getHashTruncateLast(head.getBlock()),
        head.getBlock().getSlot());
  }

  protected PeerImpl createPeer(Channel<BytesValue> channel) {
    return new PeerImpl(channel, createLocalHello(), ssz, messageSerializer, syncServer);
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
    activePeers.add(peer);
    peer.getRawChannel().getCloseFuture().thenAccept(v -> activePeers.remove(peer));
  }

  @Override
  public Collection<Peer> getActivePeers() {
    return activePeers;
  }

  @Override
  public WireApiSync getWireApiSync() {
    if (getActivePeers().isEmpty()) {
      throw new IllegalStateException("No peers connected yet. This is naive implementation");
    }
    return getActivePeers().iterator().next().getSyncApi();
  }

  @Override
  public WireApiSub getWireApiSub() {
    if (getActivePeers().isEmpty()) {
      throw new IllegalStateException("No peers connected yet. This is naive implementation");
    }
    return getActivePeers().iterator().next().getSubApi();
  }
}

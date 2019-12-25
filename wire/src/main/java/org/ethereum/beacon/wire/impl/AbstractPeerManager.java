package org.ethereum.beacon.wire.impl;

import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.ethereum.beacon.wire.Peer;
import org.ethereum.beacon.wire.PeerManager;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.message.payload.HelloMessage;
import org.ethereum.beacon.wire.sync.WireApiSyncRouter;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.util.bytes.Bytes4;

public abstract class AbstractPeerManager implements PeerManager {
  private static final Logger logger = LogManager.getLogger(AbstractPeerManager.class);

  protected final Bytes4 fork;
  protected final BeaconChainSpec spec;
  protected final Publisher<BeaconTupleDetails> headStream;
  protected final Schedulers schedulers;
  protected final WireApiSyncRouter wireApiSyncRouter;

  protected SlotNumber maxKnownSlot;
  protected final SimpleProcessor<SlotNumber> maxSlotStream;
  protected final SimpleProcessor<Peer> connectedPeersStream;

  public AbstractPeerManager(
      BeaconChainSpec spec,
      Bytes4 fork,
      Schedulers schedulers,
      Publisher<BeaconTupleDetails> headStream) {

    this.spec = spec;
    this.fork = fork;
    this.headStream = headStream;
    this.schedulers = schedulers;

    this.maxSlotStream = new SimpleProcessor<>(schedulers.events(), "PeerManager.maxSlot");
    this.connectedPeersStream = new SimpleProcessor<>(schedulers.events(), "PeerManager.connectedPeers");

    Flux.from(activatedPeerStream()).subscribe(this::onNewActivePeer);

    wireApiSyncRouter = new WireApiSyncRouter(
        Flux.from(activatedPeerStream()).map(Peer::getSyncApi),
        Flux.from(disconnectedPeerStream()).map(Peer::getSyncApi));
  }

  protected void onNewPeer(Peer peer) {
    connectedPeersStream.onNext(peer);
    updateBestSlot(peer);
  }

  protected HelloMessage createLocalHello() {
    BeaconTupleDetails head = Mono.from(headStream).block(Duration.ofSeconds(10)); // TODO
    return new HelloMessage(
        Bytes4.ZERO,
        head.getFinalState().getFinalizedCheckpoint().getRoot(),
        head.getFinalState().getFinalizedCheckpoint().getEpoch(),
        spec.getObjectHasher().getHash(head.getSignedBlock()),
        head.getSignedBlock().getMessage().getSlot());
  }

  private void updateBestSlot(Peer peer) {
    peer.getRemoteHelloMessage().thenAccept(helloMessage -> {
      if (helloMessage.getHeadSlot().greater(maxKnownSlot)) {
        maxKnownSlot = helloMessage.getHeadSlot();
        maxSlotStream.onNext(maxKnownSlot);
      }
    });
  }

  @Override
  public Publisher<Peer> connectedPeerStream() {
    return connectedPeersStream;
  }

  @Override
  public Publisher<Peer> disconnectedPeerStream() {
    return Flux.from(connectedPeersStream).flatMap(
        peer -> Mono.fromFuture(peer.getConnection().getCloseFuture().thenApply(v -> peer)));
  }

  @Override
  public Publisher<Peer> activatedPeerStream() {
    return Flux.from(connectedPeersStream).flatMap(
        peer -> Mono.fromFuture(peer.getRemoteHelloMessage().thenApply(v -> peer)));
  }

  protected void onNewActivePeer(Peer peer) {
    logger.info("New active peer: " + peer);
  }

  @Override
  public Publisher<SlotNumber> getMaxSlotStream() {
    return maxSlotStream;
  }

  @Override
  public WireApiSync getWireApiSync() {
    return wireApiSyncRouter;
  }

  @Override
  public abstract WireApiSub getWireApiSub();
}

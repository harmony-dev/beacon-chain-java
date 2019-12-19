package org.ethereum.beacon.wire.impl.libp2p;

import io.libp2p.core.Connection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.Peer;
import org.ethereum.beacon.wire.PeerConnection;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.message.payload.BlockRequestMessage;
import org.ethereum.beacon.wire.message.payload.HelloMessage;
import org.ethereum.beacon.wire.message.payload.RecentBlockRequestMessage;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class Libp2pPeer implements Peer {

  class Libp2pPeerConnection implements PeerConnection {

    @Override
    public CompletableFuture<Void> getCloseFuture() {
      return connection.closeFuture().thenApply(unit -> null);
    }

    @Override
    public void close() {
      connection.getNettyChannel().close();
    }
  }

  class Libp2pWireSync implements WireApiSync {

    @Override
    public CompletableFuture<Feedback<List<SignedBeaconBlock>>> requestBlocks(
        BlockRequestMessage requestMessage, ObjectHasher<Hash32> hasher) {
      return rpcMethods
          .blocks
          .invokeRemote(connection, requestMessage)
          .thenApply(
              resp -> Feedback.of(resp.getBlocks(), Libp2pPeer.this::invalidBlockReported));
    }

    @Override
    public CompletableFuture<Feedback<List<SignedBeaconBlock>>> requestRecentBlocks(
        List<Hash32> blockRoots,
        ObjectHasher<Hash32> hasher) {
      return rpcMethods
          .recentBlocks
          .invokeRemote(connection, new RecentBlockRequestMessage(blockRoots))
          .thenApply(
              resp -> Feedback.of(resp.getBlocks(), Libp2pPeer.this::invalidBlockReported));
    }
  }

  final Connection connection;
  private final RpcMethods rpcMethods;
  final CompletableFuture<HelloMessage> remoteHello = new CompletableFuture<>();
  private final Libp2pPeerConnection peerConnection = new Libp2pPeerConnection();
  private final Libp2pWireSync wireSync = new Libp2pWireSync();

  public Libp2pPeer(Connection connection, RpcMethods rpcMethods) {
    this.connection = connection;
    this.rpcMethods = rpcMethods;
  }

  private void invalidBlockReported(Throwable err) {

  }

  @Override
  public CompletableFuture<HelloMessage> getRemoteHelloMessage() {
    return remoteHello;
  }

  @Override
  public PeerConnection getConnection() {
    return peerConnection;
  }

  @Override
  public WireApiSync getSyncApi() {
    return wireSync;
  }

  @Override
  public WireApiSub getSubApi() {
    throw new UnsupportedOperationException(
        "SubApi is managed by Libp2p gossip and is not available on per peer basis");
  }
}

package org.ethereum.beacon.wire.impl.libp2p;

import io.libp2p.core.Connection;
import io.libp2p.core.ConnectionHandler;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.exceptions.WireRpcMalformedException;
import org.ethereum.beacon.wire.impl.AbstractPeerManager;
import org.ethereum.beacon.wire.impl.libp2p.encoding.RpcMessageCodecFactory;
import org.ethereum.beacon.wire.message.payload.GoodbyeMessage;
import org.ethereum.beacon.wire.message.payload.HelloMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes4;

public class Libp2pPeerManager extends AbstractPeerManager implements ConnectionHandler {
  private static final Logger logger = LogManager.getLogger(Libp2pPeerManager.class);

  private final WireApiSub wireApiSub;

  final RpcMethods rpcMethods;
  private volatile List<Libp2pPeer> connectedPeers = Collections.emptyList();

  public Libp2pPeerManager(
      BeaconChainSpec spec,
      Bytes4 fork,
      Schedulers schedulers,
      Publisher<BeaconTupleDetails> headStream,
      WireApiSub wireApiSub,
      RpcMessageCodecFactory codecFactory,
      WireApiSync server) {
    super(spec, fork, schedulers, headStream);

    this.wireApiSub = wireApiSub;

    rpcMethods = new RpcMethods(spec.getObjectHasher(), codecFactory, server, this::hello, this::goodbye);

    Flux.from(connectedPeersStream()).subscribe(l ->
        connectedPeers = l.stream().map(p -> (Libp2pPeer) p).collect(Collectors.toList()));
  }

  private Void goodbye(Connection connection, GoodbyeMessage message) {
    logger.info("Peer " + connection + " said goodbye: " + message);
    return null;
  }

  private HelloMessage hello(Connection connection, HelloMessage helloMessage) {
    logger.info("Peer " + connection + " said hello: " + helloMessage);
    if (connection.isInitiator()) {
      throw new WireRpcMalformedException(
          "Responder peer shouldn't initiate Hello message: " + helloMessage);
    } else {
      getPeer(connection).getRemoteHelloMessage().complete(helloMessage);
      return createLocalHello();
    }
  }

  private Libp2pPeer getPeer(Connection conn) {
    return connectedPeers.stream()
        .filter(p -> conn == p.connection)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Can't find a peer for connection: " + conn));
  }

  @Override
  public void handleConnection(Connection connection) {
    logger.info("New connection: " + connection);
    Libp2pPeer peer = new Libp2pPeer(connection, rpcMethods);
    onNewPeer(peer);
    if (connection.isInitiator()) {
      rpcMethods.hello.invokeRemote(connection, createLocalHello())
          .thenApply(resp -> peer.getRemoteHelloMessage().complete(resp));
    }
  }

  @Override
  public WireApiSub getWireApiSub() {
    return wireApiSub;
  }
}

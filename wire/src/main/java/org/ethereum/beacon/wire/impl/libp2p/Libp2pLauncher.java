package org.ethereum.beacon.wire.impl.libp2p;

import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.crypto.KEY_TYPE;
import io.libp2p.core.crypto.KeyKt;
import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.dsl.BuildersJKt;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.crypto.keys.Secp256k1Kt;
import io.libp2p.mux.mplex.MplexStreamMuxer;
import io.libp2p.protocol.Ping;
import io.libp2p.pubsub.gossip.Gossip;
import io.libp2p.security.secio.SecIoSecureChannel;
import io.libp2p.transport.tcp.TcpTransport;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.impl.libp2p.encoding.RpcMessageCodecFactory;
import org.ethereum.beacon.wire.impl.libp2p.encoding.SSZMessageCodec;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class Libp2pLauncher {
  private static final Logger logger = LogManager.getLogger(Libp2pLauncher.class);

  int listenPort = 40002;
  List<Pair<Multiaddr, PeerId>> activePeers = new ArrayList<>();
  PrivKey privKey;
  int activePeerReconnectTimeoutSec = 10;

  // TODO gossip params
  int gossipD = 3;
  int gossipDLow = 2;
  int gossipDHigh = 4;
  int gossipDGossip = 3;

  BeaconChainSpec spec;
  Bytes4 fork;
  SSZSerializer sszSerializer;
  Schedulers schedulers;
  WireApiSync wireApiSyncServer;
  Publisher<BeaconTupleDetails> headStream;

  Libp2pPeerManager peerManager;
  Host host;

  public void init() {
    if (privKey == null) {
      privKey = KeyKt.generateKeyPair(KEY_TYPE.SECP256K1).component1();
    }
    Gossip gossip = new Gossip(); // TODO gossip params
    RpcMessageCodecFactory rpcCodecFactory = SSZMessageCodec.createFactory(sszSerializer);
    WireApiSub wireApiSub = new GossipWireApiSub(sszSerializer, gossip.getApi(), privKey);
    peerManager = new Libp2pPeerManager(
        spec, fork, schedulers, headStream, wireApiSub, rpcCodecFactory, wireApiSyncServer);

    host = BuildersJKt.hostJ(
        b -> {
          b.getIdentity().setFactory(() -> privKey);
          b.getTransports().add(TcpTransport::new);
          b.getSecureChannels().add(SecIoSecureChannel::new);
          b.getMuxers().add(MplexStreamMuxer::new);
          b.getNetwork().listen("/ip4/0.0.0.0/tcp/" + listenPort);

          b.getProtocols().add(new Ping());
          b.getProtocols().add(gossip);
          b.getProtocols().addAll(peerManager.rpcMethods.all());

          b.getConnectionHandlers().add(peerManager);
        });
  }

  public CompletableFuture<Void> start() {
    logger.info("Starting libp2p network...");
    CompletableFuture<Void> ret = host.start().thenApply(i -> {
      logger.info("Listening for connections on port " + listenPort + " with peerId " + PeerId
          .fromPubKey(privKey.publicKey()).toHex());
      return null;
    });

    for (Pair<Multiaddr, PeerId> activePeer : activePeers) {
      connectActively(activePeer.getValue0(), activePeer.getValue1());
    }

    return ret;
  }

  void connectActively(Multiaddr addr, PeerId id) {
    logger.info("Connecting to " + addr + " peerId: " + id.toHex());
    host.getNetwork().connect(id, addr).whenComplete((conn, t) -> {
      if (t != null) {
        logger.info("Connection to " + addr + " failed. Will retry shortly : " + t);
        schedulers.events().executeWithDelay(Duration.ofSeconds(activePeerReconnectTimeoutSec), () -> {
          connectActively(addr, id);
        });
      } else {
        conn.closeFuture().thenAccept(ignore -> {
          logger.info("Connection to " + addr + " closed. Will retry shortly");
          schedulers.events().executeWithDelay(Duration.ofSeconds(activePeerReconnectTimeoutSec), () -> {
            connectActively(addr, id);
          });
        });
      }
    });
  }

  public void setListenPort(int listenPort) {
    this.listenPort = listenPort;
  }

  public void setPrivKey(BytesValue secp256k1PrivateKeyBytes) {
    this.privKey = Secp256k1Kt.unmarshalSecp256k1PrivateKey(secp256k1PrivateKeyBytes.getArrayUnsafe());
  }

  public void addActivePeer(String multiaddr, String hexPeerId) {
    activePeers.add(Pair.with(new Multiaddr(multiaddr), PeerId.fromHex(hexPeerId)));
  }

  public void setSpec(BeaconChainSpec spec) {
    this.spec = spec;
  }

  public void setFork(Bytes4 fork) {
    this.fork = fork;
  }

  public void setSszSerializer(SSZSerializer sszSerializer) {
    this.sszSerializer = sszSerializer;
  }

  public void setSchedulers(Schedulers schedulers) {
    this.schedulers = schedulers;
  }

  public void setWireApiSyncServer(WireApiSync wireApiSyncServer) {
    this.wireApiSyncServer = wireApiSyncServer;
  }

  public void setHeadStream(
      Publisher<BeaconTupleDetails> headStream) {
    this.headStream = headStream;
  }

  public Libp2pPeerManager getPeerManager() {
    return peerManager;
  }

  public Host getHost() {
    return host;
  }
}

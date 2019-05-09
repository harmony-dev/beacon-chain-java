package org.ethereum.beacon.wire;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.ethereum.beacon.Launcher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.simulator.SimulatorLauncher;
import org.ethereum.beacon.simulator.SimulatorLauncher.Builder;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.ethereum.beacon.wire.channel.Channel;
import org.ethereum.beacon.wire.channel.beacon.WireApiSubRpc;
import org.ethereum.beacon.wire.message.SSZMessageSerializer;
import org.ethereum.beacon.wire.net.Client;
import org.ethereum.beacon.wire.net.ConnectionManager;
import org.ethereum.beacon.wire.net.NettyClient;
import org.ethereum.beacon.wire.net.NettyServer;
import org.ethereum.beacon.wire.net.Server;
import org.ethereum.beacon.wire.sync.BeaconBlockTree;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.ethereum.beacon.wire.sync.SyncQueue;
import org.ethereum.beacon.wire.sync.SyncQueueImpl;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class PeersTest {

  @Test
  public void test0() {
    SimpleProcessor<Integer> processor = new SimpleProcessor<>(Schedulers.single(), "aaa");
    processor.onNext(1);
    Integer i = Mono.from(processor).block(Duration.ofMillis(300));
    System.out.println(i);
  }

  @Test
  public void test1() throws Exception {
    int slotCount = 1;
    SimulatorLauncher simulatorLauncher = new Builder()
        .withConfigFromResource("/sync-simulation-config.yml")
        .build();
    simulatorLauncher.run(slotCount);
    Launcher peer0 = simulatorLauncher.getPeers().get(0);
    System.out.println(peer0);

    Launcher peer1 = simulatorLauncher.createPeer("test");

    {
      // peer 0
      Server server = new NettyServer(40001);
      server.start().await();
      System.out.println("Peer 0 listening on port 40001");
      ConnectionManager<SocketAddress> connectionManager = new ConnectionManager<>(
          server, null, Schedulers.single());

      SSZSerializer ssz = new SSZBuilder().buildSerializer();
      MessageSerializer messageSerializer = new SSZMessageSerializer(ssz);
      WireApiSyncServer syncServer = new WireApiSyncServer(peer0.getBeaconChainStorage());
      SimplePeerManagerImpl peerManager = new SimplePeerManagerImpl(
          (byte) 1,
          UInt64.valueOf(1),
          connectionManager.channelsStream(),
          ssz,
          peer0.getSpec(),
          messageSerializer,
          syncServer,
          peer0.getBeaconChain().getBlockStatesStream());

      Flux.from(peerManager.connectedPeerStream())
          .subscribe(peer -> System.out.println("Remote peer connected: " + peer));
      Flux.from(peerManager.activePeerStream())
          .subscribe(peer -> System.out.println("Remote peer active: " + peer));
      Flux.from(peerManager.disconnectedPeerStream())
          .subscribe(peer -> System.out.println("Remote peer disconnected: " + peer));
      System.out.println("Peer 0 is ready.");
    }

    {
      // peer 1
      ConnectionManager<SocketAddress> connectionManager = new ConnectionManager<>(
          null, new NettyClient(), Schedulers.single());

      SSZSerializer ssz = new SSZBuilder().buildSerializer();
      MessageSerializer messageSerializer = new SSZMessageSerializer(ssz);
      SimplePeerManagerImpl peerManager = new SimplePeerManagerImpl(
          (byte) 1,
          UInt64.valueOf(1),
          connectionManager.channelsStream(),
          ssz,
          peer1.getSpec(),
          messageSerializer,
          null,
          peer1.getBeaconChain().getBlockStatesStream());
      Flux.from(peer1.getBeaconChain().getBlockStatesStream())
          .subscribe(s -> System.out.println("### " + s));

      Flux.from(peerManager.connectedPeerStream())
          .subscribe(peer -> System.out.println("Peer 1 connected: " + peer));
      Flux.from(peerManager.activePeerStream())
          .subscribe(peer -> System.out.println("Remote peer active: " + peer));
      Flux.from(peerManager.disconnectedPeerStream())
          .subscribe(peer -> System.out.println("Remote peer disconnected: " + peer));

      BeaconBlockTree blockTree = new BeaconBlockTree(simulatorLauncher.getSpec().getObjectHasher());
      SyncQueue syncQueue = new SyncQueueImpl(blockTree, 4, 16);

      SyncManager syncManager = new SyncManager(
          peer1.getBeaconChain(),
          peer1.getBeaconChainStorage(),
          peer1.getSpec(),
          peerManager.getWireApiSync(),
          syncQueue,
          1);

      AtomicBoolean synced = new AtomicBoolean();
      Flux.from(peer1.getBeaconChain().getBlockStatesStream())
          .subscribe(s -> {
            System.out.println(s);
            if (s.getFinalState().getSlot().equals(
                simulatorLauncher.getSpec().getConstants().getGenesisSlot().plus(slotCount))) {
              syncManager.stop();
              synced.set(true);
            }
          });

      System.out.println("Peer 1: starting sync manager");
      syncManager.start();

      // simulatorLauncher.getControlledSchedulers().addTime(3000);

      System.out.println("Peer 1: connecting to peer 0 for syncing...");
      CompletableFuture<Channel<BytesValue>> localhost = connectionManager
          .connect(InetSocketAddress.createUnresolved("localhost", 40001));
      localhost.get();
      System.out.println("Peer 1: connected to peer 0");


      Thread.sleep(10000000);
      Assert.assertTrue(synced.get());
      System.out.println("Done");
    }
  }
}

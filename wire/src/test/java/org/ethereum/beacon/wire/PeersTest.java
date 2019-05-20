package org.ethereum.beacon.wire;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.ethereum.beacon.start.common.Launcher;
import org.ethereum.beacon.simulator.SimulatorLauncher;
import org.ethereum.beacon.simulator.SimulatorLauncher.Builder;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.ethereum.beacon.wire.channel.Channel;
import org.ethereum.beacon.wire.message.SSZMessageSerializer;
import org.ethereum.beacon.wire.net.ConnectionManager;
import org.ethereum.beacon.wire.net.netty.NettyClient;
import org.ethereum.beacon.wire.net.netty.NettyServer;
import org.ethereum.beacon.wire.net.Server;
import org.ethereum.beacon.wire.sync.BeaconBlockTree;
import org.ethereum.beacon.wire.sync.SyncManagerImpl;
import org.ethereum.beacon.wire.sync.SyncQueue;
import org.ethereum.beacon.wire.sync.SyncQueueImpl;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
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
  public void test01() {
    ReplayProcessor<Integer> proc = ReplayProcessor.create();
    FluxSink<Integer> sink = proc.sink();

    sink.next(1);
    sink.next(2);

    Flux.from(proc)
        .onErrorContinue((t, o) -> System.out.println("Continue on error: " + t))
        .subscribe(
            i -> System.out.println("Next: " + i)
            ,t -> System.out.println("Err: " + t)
            ,() -> System.out.println("Complete")
        );

    sink.error(new RuntimeException("Test"));
    sink.next(3);
    sink.complete();
  }

  @Test
  public void test1() throws Exception {
    int slotCount = 32;
    SimulatorLauncher simulatorLauncher = new Builder()
        .withConfigFromResource("/sync-simulation-config.yml")
        .build();
    simulatorLauncher.run(slotCount);
    Launcher peer0 = simulatorLauncher.getPeers().get(0);
    System.out.println(peer0);

    Launcher peer1 = simulatorLauncher.createPeer("test");

    try (Server server = new NettyServer(40001)) {
      {
        // peer 0
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
            peer0.getSchedulers(),
            syncServer,
            peer0.getBeaconChain().getBlockStatesStream());

        Flux.from(peerManager.connectedPeerStream())
            .subscribe(
                peer -> {
                  System.out.println("Remote peer connected: " + peer);
                  Flux.from(peer.getRawChannel().inboundMessageStream())
                      .doOnError(e -> System.out.println("#### Error: " + e))
                      .doOnComplete(() -> System.out.println("#### Complete"))
                      .doOnNext(msg -> System.out.println("#### on message"))
                      .subscribe();
                });
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
            peer0.getSchedulers(),
            null,
            peer1.getBeaconChain().getBlockStatesStream());

        Flux.from(peerManager.connectedPeerStream())
            .subscribe(peer -> System.out.println("Peer 1 connected: " + peer));
        Flux.from(peerManager.activePeerStream())
            .subscribe(peer -> System.out.println("Peer 1 active: " + peer));
        Flux.from(peerManager.disconnectedPeerStream())
            .subscribe(peer -> System.out.println("Peer 1 disconnected: " + peer));

        BeaconBlockTree blockTree = new BeaconBlockTree(
            simulatorLauncher.getSpec().getObjectHasher());
        SyncQueue syncQueue = new SyncQueueImpl(blockTree, 4, 16);

        SyncManagerImpl syncManager = new SyncManagerImpl(
            peer1.getBeaconChain(),
            Flux.from(peerManager.getWireApiSub().inboundBlocksStream()).map(Feedback::of),
            peer1.getBeaconChainStorage(),
            peer1.getSpec(),
            peerManager.getWireApiSync(),
            syncQueue,
            1,
            peer1.getSchedulers().reactorEvents());

        CountDownLatch syncLatch = new CountDownLatch(1);
        Flux.from(peer1.getBeaconChain().getBlockStatesStream())
            .subscribe(s -> {
              System.out.println(s);
              if (s.getFinalState().getSlot().equals(
                  simulatorLauncher.getSpec().getConstants().getGenesisSlot().plus(slotCount))) {
                syncManager.stop();
                syncLatch.countDown();
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

        Assert.assertTrue(syncLatch.await(1, TimeUnit.MINUTES));
        System.out.println("Done");
      }
    }
  }
}

package org.ethereum.beacon.wire.sync;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.start.common.Launcher;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.simulator.SimulatorLauncher;
import org.ethereum.beacon.simulator.SimulatorLauncher.Builder;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.WireApiSyncServer;
import org.ethereum.beacon.wire.message.payload.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsResponseMessage;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;

public class SyncTest {
  private static final Logger logger = LogManager.getLogger(SyncTest.class);

  static class AsyncWireApiSync implements WireApiSync {
    WireApiSync delegate;
    Scheduler scheduler;
    Duration delay;

    public AsyncWireApiSync(WireApiSync delegate, Scheduler scheduler, Duration delay) {
      this.delegate = delegate;
      this.scheduler = scheduler;
      this.delay = delay;
    }

    @Override
    public CompletableFuture<BlockRootsResponseMessage> requestBlockRoots(
        BlockRootsRequestMessage requestMessage) {
      return scheduler.executeWithDelay(delay, () -> delegate.requestBlockRoots(requestMessage).get());
    }

    @Override
    public CompletableFuture<BlockHeadersResponseMessage> requestBlockHeaders(
        BlockHeadersRequestMessage requestMessage) {
      System.out.println("Headers requested: " + requestMessage);
      return scheduler.executeWithDelay(delay, () -> delegate.requestBlockHeaders(requestMessage).get());
    }

    @Override
    public CompletableFuture<Feedback<BlockBodiesResponseMessage>> requestBlockBodies(
        BlockBodiesRequestMessage requestMessage) {
      return scheduler.executeWithDelay(delay, () -> delegate.requestBlockBodies(requestMessage).get());
    }
  }

  @Test(timeout = 30000)
  public void test1() throws Exception {
    int slotCount = 64;
    SimulatorLauncher simulatorLauncher = new Builder()
        .withConfigFromResource("/sync-simulation-config.yml")
        .withLogLevel(null)
        .build();
    simulatorLauncher.run(slotCount);
    Launcher peer0 = simulatorLauncher.getPeers().get(0);
    System.out.println(peer0);

    Launcher testPeer = simulatorLauncher.createPeer("test");

    WireApiSyncServer syncServer = new WireApiSyncServer(peer0.getBeaconChainStorage());
    AsyncWireApiSync asyncSyncServer = new AsyncWireApiSync(syncServer,
        testPeer.getSchedulers().blocking(), Duration.ofMillis(10));

    BeaconBlockTree blockTree = new BeaconBlockTree(simulatorLauncher.getSpec().getObjectHasher());
    SyncQueue syncQueue = new SyncQueueImpl(blockTree, 4, 20);

    SyncManagerImpl syncManager = new SyncManagerImpl(
        testPeer.getBeaconChain(),
        Flux.never(),
        testPeer.getBeaconChainStorage(),
        testPeer.getSpec(),
        asyncSyncServer,
        syncQueue,
        1,
        testPeer.getSchedulers(),
        new SimpleProcessor<>(testPeer.getSchedulers().events(), "bestKnownSlotStream"));

    AtomicBoolean synced = new AtomicBoolean();
    Flux.from(testPeer.getBeaconChain().getBlockStatesStream())
        .subscribe(s -> {
          System.out.println(s);
          if (s.getFinalState().getSlot().equals(
              simulatorLauncher.getSpec().getConstants().getGenesisSlot().plus(slotCount))) {
            syncManager.stop();
            synced.set(true);
          }
        });

    System.out.println("Starting sync manager...");
    syncManager.start();

    System.out.println("Adding 3 seconds...");
    simulatorLauncher.getControlledSchedulers().addTime(5000);

    Assert.assertTrue(synced.get());
    System.out.println("Done");
  }

}

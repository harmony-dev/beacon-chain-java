package org.ethereum.beacon.wire.sync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.Launcher;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.simulator.SimulatorLauncher;
import org.ethereum.beacon.simulator.SimulatorLauncher.Builder;
import org.ethereum.beacon.wire.WireApiSyncServer;
import org.junit.Test;
import reactor.core.publisher.Flux;

public class SyncTest {
  private static final Logger logger = LogManager.getLogger(SyncTest.class);

  @Test
  public void test1() throws Exception {
    SimulatorLauncher simulatorLauncher = new Builder()
        .withConfigFromResource("/sync-simulation-config.yml")
        .build();
    simulatorLauncher.run(32);
    Launcher peer0 = simulatorLauncher.getPeers().get(0);
    System.out.println(peer0);

    WireApiSyncServer syncServer = new WireApiSyncServer(peer0.getBeaconChainStorage());

    Launcher testPeer = simulatorLauncher.createPeer("test");

    BeaconBlockTree blockTree = new BeaconBlockTree(simulatorLauncher.getSpec().getObjectHasher());
    SyncQueue syncQueue = new SyncQueueImpl(blockTree, 4, 16);

    Flux.from(testPeer.getBeaconChain().getBlockStatesStream())
        .subscribe(s -> System.out.println(s));

    SyncManager syncManager = new SyncManager(
        testPeer.getBeaconChain(),
        testPeer.getBeaconChainStorage(),
        testPeer.getSpec(),
        syncServer,
        syncQueue,
        1);
    syncManager.start();
    System.out.println("Done");
  }

}

package org.ethereum.beacon.validator.api;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.TimeResponse;
import org.ethereum.beacon.validator.api.model.VersionResponse;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReactorNettyApiTest {
  private final String SERVER_URL = "http://localhost:1234";
  private RestServer server;
  private RestClient client;

  @Before
  public void setup() {
    this.client = new RestClient(SERVER_URL);
    this.server =
        new ReactorNettyServer(
            new ObservableStateProcessor() {
              @Override
              public void start() {}

              @Override
              public Publisher<BeaconChainHead> getHeadStream() {
                return null;
              }

              @Override
              public Publisher<ObservableBeaconState> getObservableStateStream() {
                return Mono.just(
                    new ObservableBeaconState(
                        BeaconBlock.Builder.createEmpty()
                            .withSlot(SlotNumber.ZERO)
                            .withParentRoot(Hash32.ZERO)
                            .withStateRoot(Hash32.ZERO)
                            .withSignature(BLSSignature.ZERO)
                            .withBody(BeaconBlockBody.EMPTY)
                            .build(),
                        BeaconStateEx.getEmpty(),
                        PendingOperations.getEmpty()));
              }

              @Override
              public Publisher<PendingOperations> getPendingOperationsStream() {
                return null;
              }
            },
            new SyncManager() {
              @Override
              public Publisher<Feedback<BeaconBlock>> getBlocksReadyToImport() {
                return null;
              }

              @Override
              public void start() {}

              @Override
              public void stop() {}

              @Override
              public Publisher<SyncMode> getSyncModeStream() {
                return null;
              }

              @Override
              public Publisher<SyncStatus> getSyncStatusStream() {
                return Mono.just(new SyncStatus(false, null, null, null));
              }
            });
  }

  @After
  public void cleanup() {
    server.shutdown();
  }

  @Test
  public void testVersion() {
    VersionResponse response = client.getVersion();
    String version = response.getVersion();
    assertTrue(version.startsWith("Beacon"));
    assertTrue(version.contains("0."));
  }

  @Test
  public void testGenesisTime() {
    TimeResponse response = client.getGenesisTime();
    long time = response.getTime();
    assertEquals(0, time);
  }

  @Test
  public void testSyncing() {
    SyncingResponse response = client.getSyncing();
    assertFalse(response.isSyncing());
  }
}

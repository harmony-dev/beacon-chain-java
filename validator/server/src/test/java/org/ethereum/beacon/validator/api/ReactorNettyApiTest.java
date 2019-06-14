package org.ethereum.beacon.validator.api;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.TimeResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;
import org.ethereum.beacon.validator.api.model.VersionResponse;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

import javax.ws.rs.ServiceUnavailableException;

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
            BeaconChainSpec.createWithDefaults(),
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
                return Mono.just(new SyncStatus(false, null, null, null, null));
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

  @Test(expected = ServiceUnavailableException.class) // 503
  public void testValidatorDuties() {
    String pubkey1 =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    ValidatorDutiesResponse response1 = client.getValidatorDuties(0L, new String[] {pubkey1});
    assertEquals(1, response1.getValdatorDutyList().size());
    ValidatorDutiesResponse.ValidatorDuty validatorDuty = response1.getValdatorDutyList().get(0);
    assertEquals(pubkey1.toLowerCase(), validatorDuty.getValidatorPubkey().toLowerCase());
    // TODO:
    // 200 OK
    // 400 Invalid request syntax.
    // 406 Duties cannot be provided for the requested epoch.
    // 500 Beacon node internal error.
  }
}

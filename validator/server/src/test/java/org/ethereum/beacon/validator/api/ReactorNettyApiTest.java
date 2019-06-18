package org.ethereum.beacon.validator.api;

import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.api.convert.BlockDataToBlock;
import org.ethereum.beacon.validator.api.model.BlockData;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.TimeResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;
import org.ethereum.beacon.validator.api.model.VersionResponse;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
            },
            new BeaconChainProposer() {
              @Override
              public BeaconBlock propose(
                  ObservableBeaconState observableState, MessageSigner<BLSSignature> signer) {
                return null;
              }

              @Override
              public BeaconBlock.Builder prepareBuilder(
                  SlotNumber slot,
                  BLSSignature randaoReveal,
                  ObservableBeaconState observableState) {
                // TODO
                return null;
              }
            },
            new BeaconChainAttester() {
              @Override
              public Attestation attest(
                  ValidatorIndex validatorIndex,
                  ShardNumber shard,
                  ObservableBeaconState observableState,
                  MessageSigner<BLSSignature> signer) {
                return null;
              }

              @Override
              public Attestation prepareAttestation(
                  ValidatorIndex validatorIndex,
                  ShardNumber shard,
                  ObservableBeaconState observableState,
                  SlotNumber slot) {
                return null;
              }
            },
            new WireApiSub() {
              @Override
              public void sendProposedBlock(BeaconBlock block) {}

              @Override
              public void sendAttestation(Attestation attestation) {}

              @Override
              public Publisher<BeaconBlock> inboundBlocksStream() {
                return null;
              }

              @Override
              public Publisher<Attestation> inboundAttestationsStream() {
                return null;
              }
            },
            new MutableBeaconChain() {
              @Override
              public ImportResult insert(BeaconBlock block) {
                return ImportResult.NoParent;
              }

              @Override
              public Publisher<BeaconTupleDetails> getBlockStatesStream() {
                return null;
              }

              @Override
              public BeaconTuple getRecentlyProcessed() {
                return null;
              }

              @Override
              public void init() {}
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

  @Test(expected = ServiceUnavailableException.class) // 503
  public void testBlock() {
    String randaoReveal =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    BlockData response1 = client.getBlock(BigInteger.valueOf(1), randaoReveal);
    // TODO:
    // 200 OK
    // 400 Invalid request syntax.
    // 500 Beacon node internal error.
  }

  @Test
  public void testBlockSubmit() {
    BeaconBlock block =
        BeaconBlock.Builder.createEmpty()
            .withSignature(BLSSignature.ZERO)
            .withStateRoot(Hash32.ZERO)
            .withSlot(SlotNumber.ZERO)
            .withBody(BeaconBlockBody.EMPTY)
            .withParentRoot(Hash32.ZERO)
            .build();
    Response response = client.postBlock(BlockDataToBlock.serialize(block));
    assertEquals(503, response.getStatus()); // Still syncing
    // TODO:
    // 200 The block was validated successfully and has been broadcast. It has also been integrated
    // into the beacon node's database.
    // 202 The block failed validation, but was successfully broadcast anyway. It was not integrated
    // into the beacon node's database.
    // 400 Invalid request syntax.
    // 500 Beacon node internal error.
  }

  @Test(expected = ServiceUnavailableException.class) // 503
  public void testAttestation() {
    String pubKey =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    BlockData.BlockBodyData.IndexedAttestationData response1 =
        client.getAttestation(pubKey, 1L, BigInteger.ONE, 1);
    // TODO:
    // 200 OK
    // 400 Invalid request syntax.
    // 500 Beacon node internal error.
  }

  @Test
  public void testAttestationSubmit() {
    AttestationData attestationData =
        new AttestationData(
            Hash32.ZERO,
            EpochNumber.ZERO,
            Hash32.ZERO,
            EpochNumber.ZERO,
            Hash32.ZERO,
            ShardNumber.of(1),
            Hash32.ZERO,
            Hash32.ZERO);
    List<ValidatorIndex> custodyBit0Indices = new ArrayList<>();
    custodyBit0Indices.add(ValidatorIndex.of(0));
    List<ValidatorIndex> custodyBit1Indices = new ArrayList<>();
    custodyBit1Indices.add(ValidatorIndex.of(1));
    IndexedAttestation indexedAttestation =
        new IndexedAttestation(
            custodyBit0Indices, custodyBit1Indices, attestationData, BLSSignature.ZERO);
    Response response =
        client.postAttestation(BlockDataToBlock.presentIndexedAttestation(indexedAttestation));
    assertEquals(503, response.getStatus()); // Still syncing
    // TODO:
    // 200 The block was validated successfully and has been broadcast. It has also been integrated
    // into the beacon node's database.
    // 202 The block failed validation, but was successfully broadcast anyway. It was not integrated
    // into the beacon node's database.
    // 400 Invalid request syntax.
    // 500 Beacon node internal error.
  }
}

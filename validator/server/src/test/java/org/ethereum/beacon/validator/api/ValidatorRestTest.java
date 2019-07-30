package org.ethereum.beacon.validator.api;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.api.model.BlockData;
import org.ethereum.beacon.validator.api.model.ForkResponse;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;
import org.ethereum.beacon.wire.WireApiSub;
import org.junit.After;
import org.junit.Test;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.uint.UInt64;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidatorRestTest {
  private final String SERVER_HOST = "localhost";
  private final Integer SERVER_PORT = 1234;
  private final String SERVER_URL = "http://" + SERVER_HOST + ":" + SERVER_PORT;
  private ValidatorRest server;
  private ValidatorClient client = new RestClient(SERVER_URL);
  private BeaconChainSpec SPEC = BeaconChainSpec.createWithDefaults();
  private SpecConstants CONSTANTS = SPEC.getConstants();

  private ValidatorRest createSyncNotStartedServer() {
    return new ValidatorRest(
        SERVER_PORT,
        SPEC,
        ServiceFactory.createObservableStateProcessor(CONSTANTS),
        ServiceFactory.createSyncManagerSyncNotStarted(),
        UInt64.valueOf(13),
        ServiceFactory.createValidatorDutiesService(CONSTANTS),
        ServiceFactory.createWireApiSub(),
        ServiceFactory.createMutableBeaconChain());
  }

  private ValidatorRest createLongSyncServer() {
    return new ValidatorRest(
        SERVER_PORT,
        SPEC,
        ServiceFactory.createObservableStateProcessor(CONSTANTS),
        ServiceFactory.createSyncManagerSyncStarted(),
        UInt64.valueOf(13),
        ServiceFactory.createValidatorDutiesService(CONSTANTS),
        ServiceFactory.createWireApiSub(),
        ServiceFactory.createMutableBeaconChain());
  }

  private ValidatorRest createShortSyncServer() {
    return new ValidatorRest(
        SERVER_PORT,
        SPEC,
        ServiceFactory.createObservableStateProcessor(CONSTANTS),
        ServiceFactory.createSyncManagerShortSync(),
        UInt64.valueOf(13),
        ServiceFactory.createValidatorDutiesService(CONSTANTS),
        ServiceFactory.createWireApiSub(),
        ServiceFactory.createMutableBeaconChain());
  }

  @After
  public void cleanup() {
    server.stop();
  }

  @Test
  public void testVersion() {
    this.server = createSyncNotStartedServer();
    server.start();
    String version = client.getVersion();
    assertTrue(version.startsWith("Beacon"));
    assertTrue(version.contains("0."));
  }

  @Test
  public void testGenesisTime() {
    this.server =
        new ValidatorRest(
            SERVER_PORT,
            SPEC,
            ServiceFactory.createObservableStateProcessorGenesisTimeModifiedTo10(CONSTANTS),
            ServiceFactory.createSyncManagerSyncNotStarted(),
            UInt64.valueOf(13),
            ServiceFactory.createValidatorDutiesService(CONSTANTS),
            ServiceFactory.createWireApiSub(),
            ServiceFactory.createMutableBeaconChain());
    server.start();
    long time = client.getGenesisTime();
    assertEquals(10, time);
  }

  @Test
  public void testSyncing() {
    this.server = createSyncNotStartedServer();
    server.start();
    SyncingResponse response = client.getSyncing();
    assertFalse(response.isSyncing());
  }

  @Test
  public void testSyncingInProcess() {
    this.server = createLongSyncServer();
    server.start();
    SyncingResponse response = client.getSyncing();
    assertTrue(response.isSyncing());
    assertEquals(BigInteger.ZERO, response.getSyncStatus().getCurrentSlot());
    assertEquals(BigInteger.valueOf(1000), response.getSyncStatus().getHighestSlot());
  }

  @Test(expected = ServiceUnavailableException.class) // 503
  public void testValidatorDutiesSyncNotStarted() {
    this.server = createSyncNotStartedServer();
    server.start();
    String pubkey1 =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    ValidatorDutiesResponse response1 = client.getValidatorDuties(0L, new String[] {pubkey1});
  }

  @Test(expected = ServiceUnavailableException.class) // 503
  public void testValidatorDutiesLongSync() {
    this.server = createLongSyncServer();
    server.start();
    String pubkey1 =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    ValidatorDutiesResponse response1 = client.getValidatorDuties(0L, new String[] {pubkey1});
  }

  @Test(expected = BadRequestException.class) // 400
  public void testValidatorDutiesBadRequest() {
    this.server = createShortSyncServer();
    server.start();
    String pubkey1 = "0x5F18";
    ValidatorDutiesResponse response1 = client.getValidatorDuties(0L, new String[] {pubkey1});
  }

  @Test(expected = NotAcceptableException.class) // 406
  public void testValidatorDutiesBadEpoch() {
    this.server = createShortSyncServer();
    server.start();
    String pubkey1 =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    ValidatorDutiesResponse response1 = client.getValidatorDuties(2L, new String[] {pubkey1});
  }

  @Test
  public void testValidatorDuties() {
    String pubkey =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    BLSPubkey blsPubkey = BLSPubkey.fromHexString(pubkey);
    this.server =
        new ValidatorRest(
            SERVER_PORT,
            SPEC,
            ServiceFactory.createObservableStateProcessorWithValidators(blsPubkey, CONSTANTS),
            ServiceFactory.createSyncManagerShortSync(),
            UInt64.valueOf(13),
            ServiceFactory.createValidatorDutiesService(CONSTANTS),
            ServiceFactory.createWireApiSub(),
            ServiceFactory.createMutableBeaconChain());
    server.start();
    ValidatorDutiesResponse response1 = client.getValidatorDuties(0L, new String[] {pubkey});
    assertEquals(1, response1.getValidatorDutyList().size());
    ValidatorDutiesResponse.ValidatorDuty validatorDuty = response1.getValidatorDutyList().get(0);
    assertEquals(pubkey.toLowerCase(), validatorDuty.getValidatorPubkey().toLowerCase());
  }

  @Test(expected = ServiceUnavailableException.class) // 503
  public void testBlockInLongSync() {
    this.server = createLongSyncServer();
    server.start();
    String randaoReveal =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    BeaconBlock block = client.getBlock(BigInteger.valueOf(1), randaoReveal, CONSTANTS);
  }

  @Test(expected = BadRequestException.class) // 400
  public void testBlockInvalidInput() {
    this.server = createShortSyncServer();
    server.start();
    String randaoReveal = "0x5F1";
    BeaconBlock block = client.getBlock(BigInteger.valueOf(1), randaoReveal, CONSTANTS);
  }

  @Test
  public void testBlock() {
    this.server =
        new ValidatorRest(
            SERVER_PORT,
            SPEC,
            ServiceFactory.createObservableStateProcessor(CONSTANTS),
            ServiceFactory.createSyncManagerShortSync(),
            UInt64.valueOf(13),
            ServiceFactory.createValidatorDutiesService(CONSTANTS),
            ServiceFactory.createWireApiSub(),
            ServiceFactory.createMutableBeaconChain());
    server.start();
    String randaoReveal =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    BeaconBlock block = client.getBlock(BigInteger.valueOf(13), randaoReveal, CONSTANTS);
    assertEquals(13, block.getSlot().intValue());
  }

  @Test
  public void testBlockSubmitDuringSync() {
    this.server = createLongSyncServer();
    server.start();
    BeaconBlock block =
        BeaconBlock.Builder.createEmpty()
            .withSignature(BLSSignature.ZERO)
            .withStateRoot(Hash32.ZERO)
            .withSlot(SlotNumber.ZERO)
            .withBody(BeaconBlockBody.getEmpty(CONSTANTS))
            .withParentRoot(Hash32.ZERO)
            .build();
    Response response = client.postBlock(block);
    assertEquals(503, response.getStatus()); // Still syncing
  }

  @Test
  public void testBlockSubmit() {
    WireApiSub wireApiSub = ServiceFactory.createWireApiSubWithMirror();
    this.server =
        new ValidatorRest(
            SERVER_PORT,
            SPEC,
            ServiceFactory.createObservableStateProcessor(CONSTANTS),
            ServiceFactory.createSyncManagerShortSync(),
            UInt64.valueOf(13),
            ServiceFactory.createValidatorDutiesService(CONSTANTS),
            wireApiSub,
            ServiceFactory.createMutableBeaconChain());
    server.start();
    AtomicInteger wireCounter = new AtomicInteger(0);
    Flux.from(wireApiSub.inboundBlocksStream())
        .subscribe(
            b -> {
              wireCounter.incrementAndGet();
            });
    BeaconBlock block =
        BeaconBlock.Builder.createEmpty()
            .withSignature(BLSSignature.ZERO)
            .withStateRoot(Hash32.ZERO)
            .withSlot(SlotNumber.ZERO)
            .withBody(BeaconBlockBody.getEmpty(CONSTANTS))
            .withParentRoot(Hash32.ZERO)
            .build();
    Response response = client.postBlock(block);
    assertEquals(202, response.getStatus());
    // 202 The block failed validation, but was successfully broadcast anyway. It was not integrated
    // into the beacon node's database.
    assertEquals(1, wireCounter.get());
  }

  @Test(expected = ServiceUnavailableException.class) // 503
  public void testAttestationDuringSync() {
    this.server = createLongSyncServer();
    server.start();
    String pubKey =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    BlockData.BlockBodyData.IndexedAttestationData response1 =
        client.getAttestation(pubKey, 1L, BigInteger.ONE, 1);
  }

  @Test(expected = BadRequestException.class) // 400
  public void testAttestationBadInput() {
    this.server = createShortSyncServer();
    server.start();
    String pubKey = "0x";
    BlockData.BlockBodyData.IndexedAttestationData response1 =
        client.getAttestation(pubKey, 1L, BigInteger.ONE, 1);
  }

  @Test
  public void testAttestation() {
    this.server =
        new ValidatorRest(
            SERVER_PORT,
            SPEC,
            ServiceFactory.createObservableStateProcessor(CONSTANTS),
            ServiceFactory.createSyncManagerShortSync(),
            UInt64.valueOf(13),
            ServiceFactory.createValidatorDutiesService(CONSTANTS),
            ServiceFactory.createWireApiSub(),
            ServiceFactory.createMutableBeaconChain());
    server.start();
    String pubKey =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    BlockData.BlockBodyData.IndexedAttestationData response =
        client.getAttestation(pubKey, 1L, BigInteger.ONE, 14);
    assertEquals(Long.valueOf(14), response.getData().getCrosslink().getShard());
  }

  @Test
  public void testAttestationSubmitDuringSync() {
    this.server = createLongSyncServer();
    AttestationData attestationData =
        new AttestationData(Hash32.ZERO, Checkpoint.EMPTY, Checkpoint.EMPTY, Crosslink.EMPTY);
    server.start();
    List<ValidatorIndex> custodyBit0Indices = new ArrayList<>();
    custodyBit0Indices.add(ValidatorIndex.of(0));
    List<ValidatorIndex> custodyBit1Indices = new ArrayList<>();
    custodyBit1Indices.add(ValidatorIndex.of(1));
    IndexedAttestation indexedAttestation =
        new IndexedAttestation(
            custodyBit0Indices, custodyBit1Indices, attestationData, BLSSignature.ZERO, CONSTANTS);
    Response response = client.postAttestation(indexedAttestation);
    assertEquals(503, response.getStatus()); // Still syncing
  }

  @Test
  public void testAttestationSubmit() {
    WireApiSub wireApiSub = ServiceFactory.createWireApiSubWithMirror();
    String pubKey =
        "0x5F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC15105F1847060C89CB12A92AFF4EF140C9FC3A3F026796EC1510";
    this.server =
        new ValidatorRest(
            SERVER_PORT,
            SPEC,
            ServiceFactory.createObservableStateProcessorWithValidators(
                BLSPubkey.fromHexString(pubKey), CONSTANTS),
            ServiceFactory.createSyncManagerShortSync(),
            UInt64.valueOf(13),
            ServiceFactory.createValidatorDutiesService(CONSTANTS),
            wireApiSub,
            ServiceFactory.createMutableBeaconChain());
    server.start();
    AtomicInteger wireCounter = new AtomicInteger(0);
    Flux.from(wireApiSub.inboundAttestationsStream())
        .subscribe(
            b -> {
              wireCounter.incrementAndGet();
            });
    AttestationData attestationData =
        new AttestationData(Hash32.ZERO, Checkpoint.EMPTY, Checkpoint.EMPTY, Crosslink.EMPTY);
    List<ValidatorIndex> custodyBit0Indices = new ArrayList<>();
    custodyBit0Indices.add(ValidatorIndex.of(0));
    List<ValidatorIndex> custodyBit1Indices = new ArrayList<>();
    custodyBit1Indices.add(ValidatorIndex.of(1));
    IndexedAttestation indexedAttestation =
        new IndexedAttestation(
            custodyBit0Indices, custodyBit1Indices, attestationData, BLSSignature.ZERO, CONSTANTS);
    Response response = client.postAttestation(indexedAttestation);
    assertEquals(202, response.getStatus());
    // 202 The block failed validation, but was successfully broadcast anyway. It was not integrated
    // into the beacon node's database.
    assertEquals(1, wireCounter.get());
  }

  @Test
  public void testFork() {
    this.server = createSyncNotStartedServer();
    server.start();
    ForkResponse forkResponse = client.getFork();
    assertEquals(BigInteger.valueOf(13), forkResponse.getChainId());
    assertEquals(Long.valueOf(0), forkResponse.getFork().getEpoch());
    assertEquals(Bytes4.ZERO.toString(), forkResponse.getFork().getCurrentVersion());
    assertEquals(Bytes4.ZERO.toString(), forkResponse.getFork().getPreviousVersion());
  }
}

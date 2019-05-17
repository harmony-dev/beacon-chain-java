package org.ethereum.beacon.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.spec.SpecConstantsResolver;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.core.util.BeaconBlockTestUtil;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.junit.Before;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

public class ModelsSerializeTest {
  private SSZSerializer sszSerializer;
  private static SpecConstants specConstants;

  @Before
  public void setup() {
    specConstants = BeaconChainSpec.DEFAULT_CONSTANTS;
    sszSerializer = new SSZBuilder()
        .withExternalVarResolver(new SpecConstantsResolver(specConstants))
        .buildSerializer();
  }

  public static AttestationData createAttestationData() {
    AttestationData expected =
        new AttestationData(
            Hashes.sha256(BytesValue.fromHexString("aa")),
            EpochNumber.ZERO,
            Hashes.sha256(BytesValue.fromHexString("bb")),
            EpochNumber.of(123),
            Hashes.sha256(BytesValue.fromHexString("cc")),
            ShardNumber.of(345),
            Hashes.sha256(BytesValue.fromHexString("dd")),
            Hash32.ZERO);

    return expected;
  }

  @Test
  public void attestationDataTest() {
    AttestationData expected = createAttestationData();
    BytesValue encoded = sszSerializer.encode2(expected);
    AttestationData reconstructed = sszSerializer.decode(encoded, AttestationData.class);
    assertEquals(expected, reconstructed);
  }

  public static Attestation createAttestation() {
    return createAttestation(BytesValue.fromHexString("aa"));
  }

  public static Attestation createAttestation(BytesValue someValue) {
    AttestationData attestationData = createAttestationData();
    Attestation attestation =
        new Attestation(
            Bitfield.of(someValue),
            attestationData,
            Bitfield.of(BytesValue.fromHexString("bb")),
            BLSSignature.wrap(Bytes96.fromHexString("cc")));

    return attestation;
  }

  @Test
  public void attestationTest() {
    Attestation expected = createAttestation();
    BytesValue encoded = sszSerializer.encode2(expected);
    Attestation reconstructed = sszSerializer.decode(encoded, Attestation.class);
    assertEquals(expected, reconstructed);
  }

  private static DepositData createDepositData() {
    DepositData depositData =
        new DepositData(
            BLSPubkey.wrap(Bytes48.TRUE),
            Hashes.sha256(BytesValue.fromHexString("aa")),
            Gwei.ZERO, BLSSignature.wrap(Bytes96.ZERO));

    return depositData;
  }

  @Test
  public void depositDataTest() {
    DepositData expected = createDepositData();
    BytesValue encoded = sszSerializer.encode2(expected);
    DepositData reconstructed = sszSerializer.decode(encoded, DepositData.class);
    assertEquals(expected, reconstructed);
  }

  public static Deposit createDeposit1() {
    Deposit deposit = new Deposit(
        ReadVector.wrap(
            Collections.nCopies(specConstants.getDepositContractTreeDepth().getIntValue(), Hash32.ZERO), Integer::new),
        UInt64.ZERO, createDepositData());

    return deposit;
  }

  public static Deposit createDeposit2() {
    ArrayList<Hash32> hashes = new ArrayList<>();
    hashes.add(Hashes.sha256(BytesValue.fromHexString("aa")));
    hashes.add(Hashes.sha256(BytesValue.fromHexString("bb")));
    hashes.addAll(Collections.nCopies(specConstants.getDepositContractTreeDepth().getIntValue() - hashes.size(), Hash32.ZERO));
    ReadVector<Integer, Hash32> proof = ReadVector.wrap(hashes, Integer::new);
    Deposit deposit = new Deposit(proof, UInt64.ZERO, createDepositData());

    return deposit;
  }

  @Test
  public void depositTest() {
    Deposit expected1 = createDeposit1();
    Deposit expected2 = createDeposit2();
    BytesValue encoded1 = sszSerializer.encode2(expected1);
    BytesValue encoded2 = sszSerializer.encode2(expected2);
    Deposit reconstructed1 = sszSerializer.decode(encoded1, Deposit.class);
    Deposit reconstructed2 = sszSerializer.decode(encoded2, Deposit.class);
    assertEquals(expected1, reconstructed1);
    assertEquals(expected2, reconstructed2);
  }

  public static VoluntaryExit createExit() {
    VoluntaryExit voluntaryExit = new VoluntaryExit(EpochNumber.of(123), ValidatorIndex.MAX, BLSSignature.wrap(Bytes96.fromHexString("aa")));

    return voluntaryExit;
  }

  @Test
  public void exitTest() {
    VoluntaryExit expected = createExit();
    BytesValue encoded = sszSerializer.encode2(expected);
    VoluntaryExit reconstructed = sszSerializer.decode(encoded, VoluntaryExit.class);
    assertEquals(expected, reconstructed);
  }

  @Test
  public void beaconBlockHeaderTest() {
    Random random = new Random();
    BeaconBlockHeader expected = BeaconBlockTestUtil.createRandomHeader(random);
    BytesValue encoded = sszSerializer.encode2(expected);
    BeaconBlockHeader reconstructed = sszSerializer.decode(encoded, BeaconBlockHeader.class);
    assertEquals(expected, reconstructed);
  }

  public static  ProposerSlashing createProposerSlashing(Random random) {
    ProposerSlashing proposerSlashing =
        new ProposerSlashing(
            ValidatorIndex.MAX,
            BeaconBlockTestUtil.createRandomHeader(random),
            BeaconBlockTestUtil.createRandomHeader(random));

    return proposerSlashing;
  }

  @Test
  public void proposerSlashingTest() {
    Random random = new Random(1);
    ProposerSlashing expected = createProposerSlashing(random);
    BytesValue encoded = sszSerializer.encode2(expected);
    ProposerSlashing reconstructed = sszSerializer.decode(encoded, ProposerSlashing.class);
    assertEquals(expected, reconstructed);
  }

  public static BeaconBlockBody createBeaconBlockBody() {
    Random random = new Random(1);
    List<ProposerSlashing> proposerSlashings = new ArrayList<>();
    proposerSlashings.add(createProposerSlashing(random));
    List<AttesterSlashing> attesterSlashings = new ArrayList<>();
    attesterSlashings.add(createAttesterSlashings());
    attesterSlashings.add(createAttesterSlashings());
    List<Attestation> attestations = new ArrayList<>();
    attestations.add(createAttestation());
    List<Deposit> deposits = new ArrayList<>();
    deposits.add(createDeposit1());
    deposits.add(createDeposit2());
    List<VoluntaryExit> voluntaryExits = new ArrayList<>();
    voluntaryExits.add(createExit());
    List<Transfer> transfers = new ArrayList<>();
    BeaconBlockBody beaconBlockBody =
        BeaconBlockBody.create(
            BLSSignature.ZERO,
            new Eth1Data(Hash32.ZERO, UInt64.ZERO, Hash32.ZERO),
            Bytes32.ZERO,
            proposerSlashings,
            attesterSlashings,
            attestations,
            deposits,
            voluntaryExits,
            transfers
            );

    return beaconBlockBody;
  }

  public static AttesterSlashing createAttesterSlashings() {
    return new AttesterSlashing(
        createSlashableAttestation(),
        createSlashableAttestation());
  }

  private static IndexedAttestation createSlashableAttestation() {
    return new IndexedAttestation(
        Arrays.asList(ValidatorIndex.of(234), ValidatorIndex.of(235)),
        Arrays.asList(ValidatorIndex.of(678), ValidatorIndex.of(679)),
        createAttestationData(),
        BLSSignature.wrap(Bytes96.fromHexString("aa")));
  }

  @Test
  public void slashableAttestationTest() {
    IndexedAttestation expected = createSlashableAttestation();
    BytesValue encoded = sszSerializer.encode2(expected);
    IndexedAttestation reconstructed = sszSerializer.decode(encoded, IndexedAttestation.class);
    assertEquals(expected, reconstructed);
  }

  @Test
  public void attesterSlashingTest() {
    AttesterSlashing expected = createAttesterSlashings();
    BytesValue encoded = sszSerializer.encode2(expected);
    AttesterSlashing reconstructed = sszSerializer.decode(encoded, AttesterSlashing.class);
    assertEquals(expected, reconstructed);
  }

  @Test
  public void beaconBlockBodyTest() {
    BeaconBlockBody expected = createBeaconBlockBody();
    BytesValue encoded = sszSerializer.encode2(expected);
    BeaconBlockBody reconstructed = sszSerializer.decode(encoded, BeaconBlockBody.class);
    assertEquals(expected, reconstructed);
  }

  public static BeaconBlock createBeaconBlock() {
    return createBeaconBlock(BytesValue.fromHexString("aa"));
  }

  public static BeaconBlock createBeaconBlock(BytesValue someValue) {
    BeaconBlock beaconBlock =
        new BeaconBlock(
            SlotNumber.castFrom(UInt64.MAX_VALUE),
            Hashes.sha256(someValue),
            Hashes.sha256(BytesValue.fromHexString("bb")),
            createBeaconBlockBody(),
            BLSSignature.wrap(Bytes96.fromHexString("aa")));

    return beaconBlock;
  }

  @Test
  public void beaconBlockTest() {
    BeaconBlock expected = createBeaconBlock();
    BytesValue encoded = sszSerializer.encode2(expected);
    BeaconBlock reconstructed = sszSerializer.decode(encoded, BeaconBlock.class);
    assertEquals(expected, reconstructed);
  }

  private BeaconState createBeaconState() {
    BeaconState beaconState = BeaconState.getEmpty();

    return beaconState;
  }

  @Test
  public void beaconStateTest() {
    BeaconState expected = createBeaconState();
    BytesValue encoded = sszSerializer.encode2(expected);
    BeaconState reconstructed = sszSerializer.decode(encoded, BeaconStateImpl.class);
    assertEquals(expected, reconstructed);
  }

  @Test
  public void beaconStateExTest() {
    BeaconState expected = createBeaconState();
    BeaconStateEx stateEx = new BeaconStateExImpl(expected);
    BytesValue encoded = sszSerializer.encode2(stateEx);
    BeaconState reconstructed = sszSerializer.decode(encoded, BeaconStateImpl.class);
    assertEquals(expected, reconstructed);
  }

  private Crosslink createCrosslink() {
    Crosslink crosslink = Crosslink.EMPTY;

    return crosslink;
  }

  @Test
  public void crosslinkTest() {
    Crosslink expected = createCrosslink();
    BytesValue encoded = sszSerializer.encode2(expected);
    Crosslink reconstructed = sszSerializer.decode(encoded, Crosslink.class);
    assertEquals(expected, reconstructed);
  }

  private Eth1DataVote createEth1DataVote() {
    Eth1DataVote eth1DataVote = new Eth1DataVote(Eth1Data.EMPTY, UInt64.MAX_VALUE);

    return eth1DataVote;
  }

  @Test
  public void eth1DataVoteTest() {
    Eth1DataVote expected = createEth1DataVote();
    BytesValue encoded = sszSerializer.encode2(expected);
    Eth1DataVote reconstructed = sszSerializer.decode(encoded, Eth1DataVote.class);
    assertEquals(expected, reconstructed);
  }

  private Fork createFork() {
    Fork fork = Fork.EMPTY;

    return fork;
  }

  @Test
  public void forkTest() {
    Fork expected = createFork();
    BytesValue encoded = sszSerializer.encode2(expected);
    Fork reconstructed = sszSerializer.decode(encoded, Fork.class);
    assertEquals(expected, reconstructed);
  }

  private PendingAttestation createPendingAttestation() {
    PendingAttestation pendingAttestation =
        new PendingAttestation(
            Bitfield.of(BytesValue.fromHexString("aa")),
            createAttestationData(),
            SlotNumber.ZERO,
            ValidatorIndex.ZERO);

    return pendingAttestation;
  }

  @Test
  public void pendingAttestationTest() {
    PendingAttestation expected = createPendingAttestation();
    BytesValue encoded = sszSerializer.encode2(expected);
    PendingAttestation reconstructed =
        sszSerializer.decode(encoded, PendingAttestation.class);
    assertEquals(expected, reconstructed);
  }

  private ValidatorRecord createValidatorRecord() {
    ValidatorRecord validatorRecord =
        ValidatorRecord.Builder.fromDepositData(createDepositData())
            .withPubKey(BLSPubkey.ZERO)
            .withWithdrawalCredentials(Hash32.ZERO)
            .withActivationEligibilityEpoch(EpochNumber.ZERO)
            .withActivationEpoch(EpochNumber.ZERO)
            .withExitEpoch(EpochNumber.ZERO)
            .withWithdrawableEpoch(EpochNumber.ZERO)
            .withSlashed(Boolean.FALSE)
            .withEffectiveBalance(Gwei.ZERO)
            .build();

    return validatorRecord;
  }

  @Test
  public void validatorRecordTest() {
    ValidatorRecord expected = createValidatorRecord();
    BytesValue encoded = sszSerializer.encode2(expected);
    ValidatorRecord reconstructed = sszSerializer.decode(encoded, ValidatorRecord.class);
    assertEquals(expected, reconstructed);
  }
}

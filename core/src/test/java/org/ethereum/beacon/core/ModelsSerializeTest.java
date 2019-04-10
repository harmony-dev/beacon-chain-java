package org.ethereum.beacon.core;

import java.util.Random;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.SlashableAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.ssz.DefaultSSZ;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
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
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.core.util.BeaconBlockTestUtil;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.access.basic.BytesCodec;
import org.ethereum.beacon.ssz.access.basic.HashCodec;
import org.ethereum.beacon.ssz.access.basic.UIntCodec;
import org.ethereum.beacon.ssz.access.list.BytesValueAccessor;
import org.ethereum.beacon.ssz.access.list.ReadListAccessor;
import org.junit.Before;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ModelsSerializeTest {
  private SSZSerializer sszSerializer;

  @Before
  public void setup() {
    sszSerializer = DefaultSSZ.createCommonSSZBuilder(new SpecConstants(){})
        .buildSerializer();
  }

  private AttestationData createAttestationData() {
    AttestationData expected =
        new AttestationData(
            SlotNumber.of(123),
            Hashes.keccak256(BytesValue.fromHexString("aa")),
            EpochNumber.ZERO,
            Hashes.keccak256(BytesValue.fromHexString("bb")),
            Hashes.keccak256(BytesValue.fromHexString("cc")),
            ShardNumber.of(345),
            new Crosslink(EpochNumber.ZERO, Hashes.keccak256(BytesValue.fromHexString("dd"))),
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

  private Attestation createAttestation() {
    AttestationData attestationData = createAttestationData();
    Attestation attestation =
        new Attestation(
            Bitfield.of(BytesValue.fromHexString("aa")),
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

  private DepositInput createDepositInput() {
    DepositInput depositInput =
        new DepositInput(
            BLSPubkey.wrap(Bytes48.TRUE),
            Hashes.keccak256(BytesValue.fromHexString("aa")),
            BLSSignature.wrap(Bytes96.ZERO));

    return depositInput;
  }

  @Test
  public void depositInputTest() {
    DepositInput expected = createDepositInput();
    BytesValue encoded = sszSerializer.encode2(expected);
    DepositInput reconstructed = sszSerializer.decode(encoded, DepositInput.class);
    assertEquals(expected, reconstructed);
  }

  private DepositData createDepositData() {
    DepositData depositData =
        new DepositData(Gwei.ZERO, Time.castFrom(UInt64.valueOf(123)), createDepositInput());

    return depositData;
  }

  @Test
  public void depositDataTest() {
    DepositData expected = createDepositData();
    BytesValue encoded = sszSerializer.encode2(expected);
    DepositData reconstructed = sszSerializer.decode(encoded, DepositData.class);
    assertEquals(expected, reconstructed);
  }

  private Deposit createDeposit1() {
    Deposit deposit = new Deposit(Collections.emptyList(), UInt64.ZERO, createDepositData());

    return deposit;
  }

  private Deposit createDeposit2() {
    ArrayList<Hash32> hashes = new ArrayList<>();
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("aa")));
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("bb")));
    Deposit deposit = new Deposit(hashes, UInt64.ZERO, createDepositData());

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

  private VoluntaryExit createExit() {
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

  private ProposerSlashing createProposerSlashing(Random random) {
    ProposerSlashing proposerSlashing =
        new ProposerSlashing(
            ValidatorIndex.MAX,
            BeaconBlockTestUtil.createRandomHeader(random),
            BeaconBlockTestUtil.createRandomHeader(random));

    return proposerSlashing;
  }

  @Test
  public void proposerSlashingTest() {
    Random random = new Random();
    ProposerSlashing expected = createProposerSlashing(random);
    BytesValue encoded = sszSerializer.encode2(expected);
    ProposerSlashing reconstructed = sszSerializer.decode(encoded, ProposerSlashing.class);
    assertEquals(expected, reconstructed);
  }

  private BeaconBlockBody createBeaconBlockBody() {
    Random random = new Random();
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
        new BeaconBlockBody(
            BLSSignature.ZERO,
            new Eth1Data(Hash32.ZERO, Hash32.ZERO),
            proposerSlashings,
            attesterSlashings,
            attestations,
            deposits,
            voluntaryExits,
            transfers
            );

    return beaconBlockBody;
  }

  private AttesterSlashing createAttesterSlashings() {
    return new AttesterSlashing(
        createSlashableAttestation(),
        createSlashableAttestation());
  }

  private SlashableAttestation createSlashableAttestation() {
    return new SlashableAttestation(
        Arrays.asList(ValidatorIndex.of(234), ValidatorIndex.of(678)),
        createAttestationData(),
        Bitfield.of(BytesValue.fromHexString("aa19")),
        BLSSignature.wrap(Bytes96.fromHexString("aa")));
  }

  @Test
  public void slashableAttestationTest() {
    SlashableAttestation expected = createSlashableAttestation();
    BytesValue encoded = sszSerializer.encode2(expected);
    SlashableAttestation reconstructed = sszSerializer.decode(encoded, SlashableAttestation.class);
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

  private BeaconBlock createBeaconBlock() {
    BeaconBlock beaconBlock =
        new BeaconBlock(
            SlotNumber.castFrom(UInt64.MAX_VALUE),
            Hashes.keccak256(BytesValue.fromHexString("aa")),
            Hashes.keccak256(BytesValue.fromHexString("bb")),
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
    BeaconStateEx stateEx = new BeaconStateExImpl(expected, Hash32.ZERO);
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
            Bitfield.of(BytesValue.fromHexString("bb")),
            SlotNumber.ZERO);

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
        ValidatorRecord.Builder.fromDepositInput(createDepositInput())
            .withPubKey(BLSPubkey.ZERO)
            .withWithdrawalCredentials(Hash32.ZERO)
            .withActivationEpoch(EpochNumber.ZERO)
            .withExitEpoch(EpochNumber.ZERO)
            .withWithdrawableEpoch(EpochNumber.ZERO)
            .withInitiatedExit(Boolean.FALSE)
            .withExitEpoch(EpochNumber.ZERO)
            .withSlashed(Boolean.FALSE)
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

package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.Proposal;
import org.ethereum.beacon.core.operations.slashing.SlashableAttestation;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
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
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.Serializer;
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
  private Serializer sszSerializer;

  @Before
  public void setup() {
    sszSerializer = Serializer.annotationSerializer();
  }

  private AttestationData createAttestationData() {
    AttestationData expected =
        new AttestationData(
            SlotNumber.of(123),
            ShardNumber.of(345),
            Hashes.keccak256(BytesValue.fromHexString("aa")),
            Hashes.keccak256(BytesValue.fromHexString("bb")),
            Hashes.keccak256(BytesValue.fromHexString("cc")),
            Hashes.keccak256(BytesValue.fromHexString("dd")),
            EpochNumber.ZERO,
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
            attestationData,
            Bitfield.of(BytesValue.fromHexString("aa")),
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

  private Exit createExit() {
    Exit exit = new Exit(EpochNumber.of(123), ValidatorIndex.MAX, BLSSignature.wrap(Bytes96.fromHexString("aa")));

    return exit;
  }

  @Test
  public void exitTest() {
    Exit expected = createExit();
    BytesValue encoded = sszSerializer.encode2(expected);
    Exit reconstructed = sszSerializer.decode(encoded, Exit.class);
    assertEquals(expected, reconstructed);
  }

  private Proposal createProposalSignedData() {
    Proposal proposal =
        new Proposal(
            SlotNumber.of(12),
            ShardNumber.ZERO,
            Hashes.keccak256(BytesValue.fromHexString("aa")),
            BLSSignature.ZERO);
    return proposal;
  }

  @Test
  public void proposalSignedDataTest() {
    Proposal expected = createProposalSignedData();
    BytesValue encoded = sszSerializer.encode2(expected);
    Proposal reconstructed = sszSerializer.decode(encoded, Proposal.class);
    assertEquals(expected, reconstructed);
  }

  private ProposerSlashing createProposerSlashing() {
    ProposerSlashing proposerSlashing =
        new ProposerSlashing(
            ValidatorIndex.MAX,
            createProposalSignedData(),
            BLSSignature.wrap(Bytes96.fromHexString("aa")),
            createProposalSignedData(),
            BLSSignature.wrap(Bytes96.fromHexString("bb")));

    return proposerSlashing;
  }

  @Test
  public void proposerSlashingTest() {
    ProposerSlashing expected = createProposerSlashing();
    BytesValue encoded = sszSerializer.encode2(expected);
    ProposerSlashing reconstructed = sszSerializer.decode(encoded, ProposerSlashing.class);
    assertEquals(expected, reconstructed);
  }

  private BeaconBlockBody createBeaconBlockBody() {
    List<ProposerSlashing> proposerSlashings = new ArrayList<>();
    proposerSlashings.add(createProposerSlashing());
    List<AttesterSlashing> attesterSlashings = new ArrayList<>();
    attesterSlashings.add(createAttesterSlashings());
    attesterSlashings.add(createAttesterSlashings());
    List<Attestation> attestations = new ArrayList<>();
    attestations.add(createAttestation());
    List<Deposit> deposits = new ArrayList<>();
    deposits.add(createDeposit1());
    deposits.add(createDeposit2());
    List<Exit> exits = new ArrayList<>();
    exits.add(createExit());
    BeaconBlockBody beaconBlockBody =
        new BeaconBlockBody(
            proposerSlashings,
            attesterSlashings,
            attestations,
            deposits,
            exits);

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
            BLSSignature.wrap(Bytes96.fromHexString("cc")),
            new Eth1Data(
                Hashes.keccak256(BytesValue.fromHexString("ddaa")),
                Hashes.keccak256(BytesValue.fromHexString("ddbb"))),
            BLSSignature.wrap(Bytes96.fromHexString("aa")),
            createBeaconBlockBody());

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

  private CrosslinkRecord createCrosslinkRecord() {
    CrosslinkRecord crosslinkRecord = CrosslinkRecord.EMPTY;

    return crosslinkRecord;
  }

  @Test
  public void crosslinkRecordTest() {
    CrosslinkRecord expected = createCrosslinkRecord();
    BytesValue encoded = sszSerializer.encode2(expected);
    CrosslinkRecord reconstructed = sszSerializer.decode(encoded, CrosslinkRecord.class);
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

  private ForkData createForkData() {
    ForkData forkData = ForkData.EMPTY;

    return forkData;
  }

  @Test
  public void forkDataTest() {
    ForkData expected = createForkData();
    BytesValue encoded = sszSerializer.encode2(expected);
    ForkData reconstructed = sszSerializer.decode(encoded, ForkData.class);
    assertEquals(expected, reconstructed);
  }

  private PendingAttestationRecord createPendingAttestationRecord() {
    PendingAttestationRecord pendingAttestationRecord =
        new PendingAttestationRecord(
            Bitfield.of(BytesValue.fromHexString("aa")),
            createAttestationData(),
            Bitfield.of(BytesValue.fromHexString("bb")),
            SlotNumber.ZERO);

    return pendingAttestationRecord;
  }

  @Test
  public void pendingAttestationRecordTest() {
    PendingAttestationRecord expected = createPendingAttestationRecord();
    BytesValue encoded = sszSerializer.encode2(expected);
    PendingAttestationRecord reconstructed =
        sszSerializer.decode(encoded, PendingAttestationRecord.class);
    assertEquals(expected, reconstructed);
  }

  private ValidatorRecord createValidatorRecord() {
    ValidatorRecord validatorRecord =
        ValidatorRecord.Builder.fromDepositInput(createDepositInput())
            .withPubKey(BLSPubkey.ZERO)
            .withWithdrawalCredentials(Hash32.ZERO)
            .withActivationEpoch(EpochNumber.ZERO)
            .withExitEpoch(EpochNumber.ZERO)
            .withWithdrawalEpoch(EpochNumber.ZERO)
            .withPenalizedEpoch(EpochNumber.ZERO)
            .withExitEpoch(EpochNumber.ZERO)
            .withStatusFlags(UInt64.ZERO)
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

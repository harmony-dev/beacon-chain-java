package org.ethereum.beacon.core;

import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.operations.CustodyReseed;
import org.ethereum.beacon.core.operations.CustodyResponse;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.Serializer;
import org.junit.Before;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

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
            SlotNumber.ZERO,
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
            BytesValue.fromHexString("aa"),
            BytesValue.fromHexString("bb"),
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

  private SlashableVoteData createSlashableVoteData() {
    List<ValidatorIndex> custodyBit0Indices = Collections.emptyList();
    List<ValidatorIndex> custodyBit1Indices = Arrays.asList(ValidatorIndex.of(123), ValidatorIndex.MAX);
    AttestationData data = createAttestationData();
    BLSSignature aggregatedSignature = BLSSignature.wrap(Bytes96.fromHexString("aabbccdd"));
    SlashableVoteData slashableVoteData =
        new SlashableVoteData(custodyBit0Indices, custodyBit1Indices, data, aggregatedSignature);
    return slashableVoteData;
  }

  @Test
  public void slashableVoteDataTest() {
    SlashableVoteData expected = createSlashableVoteData();
    BytesValue encoded = sszSerializer.encode2(expected);
    SlashableVoteData reconstructed = sszSerializer.decode(encoded, SlashableVoteData.class);
    assertEquals(expected, reconstructed);
  }

  private CasperSlashing createCasperSlashing() {
    CasperSlashing casperSlashing =
        new CasperSlashing(createSlashableVoteData(), createSlashableVoteData());
    return casperSlashing;
  }

  @Test
  public void casperSlashingTest() {
    CasperSlashing expected = createCasperSlashing();
    BytesValue encoded = sszSerializer.encode2(expected);
    CasperSlashing reconstructed = sszSerializer.decode(encoded, CasperSlashing.class);
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
        new DepositData(createDepositInput(), Gwei.ZERO, UInt64.valueOf(123));

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
    Exit exit = new Exit(SlotNumber.of(123), ValidatorIndex.MAX, BLSSignature.wrap(Bytes96.fromHexString("aa")));

    return exit;
  }

  @Test
  public void exitTest() {
    Exit expected = createExit();
    BytesValue encoded = sszSerializer.encode2(expected);
    Exit reconstructed = sszSerializer.decode(encoded, Exit.class);
    assertEquals(expected, reconstructed);
  }

  private CustodyChallenge createProofOfCustodyChallenge() {
    CustodyChallenge proofOfCustodyChallenge = new CustodyChallenge();
    return proofOfCustodyChallenge;
  }

  @Test
  public void proofOfCustodyChallengeTest() {
    CustodyChallenge expected = createProofOfCustodyChallenge();
    BytesValue encoded = sszSerializer.encode2(expected);
    CustodyChallenge reconstructed = sszSerializer.decode(encoded, CustodyChallenge.class);
    assertEquals(expected, reconstructed);
  }

  private CustodyResponse createProofOfCustodyResponse() {
    CustodyResponse proofOfCustodyResponse = new CustodyResponse();
    return proofOfCustodyResponse;
  }

  @Test
  public void proofOfCustodyResponseTest() {
    CustodyResponse expected = createProofOfCustodyResponse();
    BytesValue encoded = sszSerializer.encode2(expected);
    CustodyResponse reconstructed = sszSerializer.decode(encoded, CustodyResponse.class);
    assertEquals(expected, reconstructed);
  }

  private CustodyReseed createProofOfCustodySeedChange() {
    CustodyReseed proofOfCustodySeedChange = new CustodyReseed();
    return proofOfCustodySeedChange;
  }

  @Test
  public void proofOfCustodySeedChangeTest() {
    CustodyReseed expected = createProofOfCustodySeedChange();
    BytesValue encoded = sszSerializer.encode2(expected);
    CustodyReseed reconstructed = sszSerializer.decode(encoded, CustodyReseed.class);
    assertEquals(expected, reconstructed);
  }

  private ProposalSignedData createProposalSignedData() {
    ProposalSignedData proposalSignedData =
        new ProposalSignedData(
            SlotNumber.of(12), ShardNumber.ZERO, Hashes.keccak256(BytesValue.fromHexString("aa")));
    return proposalSignedData;
  }

  @Test
  public void proposalSignedDataTest() {
    ProposalSignedData expected = createProposalSignedData();
    BytesValue encoded = sszSerializer.encode2(expected);
    ProposalSignedData reconstructed = sszSerializer.decode(encoded, ProposalSignedData.class);
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
    List<CasperSlashing> casperSlashings = new ArrayList<>();
    casperSlashings.add(createCasperSlashing());
    casperSlashings.add(createCasperSlashing());
    List<Attestation> attestations = new ArrayList<>();
    attestations.add(createAttestation());
    List<CustodyReseed> pocSeedChanges = new ArrayList<>();
    List<CustodyChallenge> pocChallenges = new ArrayList<>();
    pocChallenges.add(createProofOfCustodyChallenge());
    List<CustodyResponse> pocResponses = new ArrayList<>();
    pocResponses.add(createProofOfCustodyResponse());
    List<Deposit> deposits = new ArrayList<>();
    deposits.add(createDeposit1());
    deposits.add(createDeposit2());
    List<Exit> exits = new ArrayList<>();
    exits.add(createExit());
    BeaconBlockBody beaconBlockBody =
        new BeaconBlockBody(
            proposerSlashings,
            casperSlashings,
            attestations,
            pocSeedChanges,
            pocChallenges,
            pocResponses,
            deposits,
            exits);

    return beaconBlockBody;
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
                Hashes.keccak256(BytesValue.fromHexString("dda")),
                Hashes.keccak256(BytesValue.fromHexString("ddb"))),
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
            createAttestationData(),
            BytesValue.fromHexString("aa"),
            BytesValue.fromHexString("bb"),
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
            .withActivationSlot(SlotNumber.ZERO)
            .withExitSlot(SlotNumber.ZERO)
            .withWithdrawalSlot(SlotNumber.ZERO)
            .withPenalizedSlot(SlotNumber.ZERO)
            .withExitCount(UInt64.ZERO)
            .withStatusFlags(UInt64.ZERO)
            .withLatestCustodyReseedSlot(UInt64.ZERO)
            .withPenultimateCustodyReseedSlot(UInt64.ZERO)
            .withProposerSlots(SlotNumber.ZERO)
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

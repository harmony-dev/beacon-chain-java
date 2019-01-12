package org.ethereum.beacon.core;

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
import org.ethereum.beacon.types.ssz.Serializer;
import org.ethereum.beacon.crypto.Hashes;
import org.junit.Before;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ModelsSerializeTest {
  private Serializer sszSerializer;

  @Before
  public void setup() {
    sszSerializer = Serializer.annotationSerializer();
  }

  private AttestationData createAttestationData() {
    AttestationData expected = new AttestationData(
        UInt64.valueOf(123),
        UInt64.valueOf(345),
        Hashes.keccak256(BytesValue.fromHexString("aa")),
        Hashes.keccak256(BytesValue.fromHexString("bb")),
        Hashes.keccak256(BytesValue.fromHexString("cc")),
        Hashes.keccak256(BytesValue.fromHexString("dd")),
        UInt64.ZERO,
        Hash32.ZERO
    );

    return expected;
  }

  @Test
  public void attestationDataTest() {
    AttestationData expected = createAttestationData();
    BytesValue encoded = sszSerializer.encode2(expected);
    AttestationData reconstructed = (AttestationData) sszSerializer.decode(encoded, AttestationData.class);
    assertEquals(expected, reconstructed);
  }

  private Attestation createAttestation() {
    AttestationData attestationData = createAttestationData();
    Attestation attestation = new Attestation(
        attestationData,
        BytesValue.fromHexString("aa"),
        BytesValue.fromHexString("bb"),
        Bytes96.fromHexString("cc")
    );

    return attestation;
  }

  @Test
  public void attestationTest() {
    Attestation expected = createAttestation();
    BytesValue encoded = sszSerializer.encode2(expected);
    Attestation reconstructed = (Attestation) sszSerializer.decode(encoded, Attestation.class);
    assertEquals(expected, reconstructed);
  }

  private SlashableVoteData createSlashableVoteData() {
    UInt24[] custodyBit0Indices = new UInt24[0];
    UInt24[] custodyBit1Indices = new UInt24[]{UInt24.valueOf(123), UInt24.MAX_VALUE};
    AttestationData data = createAttestationData();
    Bytes96 aggregatedSignature = Bytes96.fromHexString("aabbccdd");
    SlashableVoteData slashableVoteData = new SlashableVoteData(custodyBit0Indices, custodyBit1Indices,
        data, aggregatedSignature);
    return slashableVoteData;
  }

  @Test
  public void slashableVoteDataTest() {
    SlashableVoteData expected = createSlashableVoteData();
    BytesValue encoded = sszSerializer.encode2(expected);
    SlashableVoteData reconstructed = (SlashableVoteData) sszSerializer.decode(encoded, SlashableVoteData.class);
    assertEquals(expected, reconstructed);
  }

  private CasperSlashing createCasperSlashing() {
    CasperSlashing casperSlashing = new CasperSlashing(
        createSlashableVoteData(),
        createSlashableVoteData()
    );
    return casperSlashing;
  }

  @Test
  public void casperSlashingTest() {
    CasperSlashing expected = createCasperSlashing();
    BytesValue encoded = sszSerializer.encode2(expected);
    CasperSlashing reconstructed = (CasperSlashing) sszSerializer.decode(encoded, CasperSlashing.class);
    assertEquals(expected, reconstructed);
  }

  private DepositInput createDepositInput() {
    DepositInput depositInput = new DepositInput(
        Bytes48.TRUE,
        Hashes.keccak256(BytesValue.fromHexString("aa")),
        Hashes.keccak256(BytesValue.fromHexString("bb")),
        Hashes.keccak256(BytesValue.fromHexString("cc")),
        Bytes96.ZERO
    );

    return depositInput;
  }

  @Test
  public void depositInputTest() {
    DepositInput expected = createDepositInput();
    BytesValue encoded = sszSerializer.encode2(expected);
    DepositInput reconstructed = (DepositInput) sszSerializer.decode(encoded, DepositInput.class);
    assertEquals(expected, reconstructed);
  }

  private DepositData createDepositData() {
    DepositData depositData = new DepositData(
        createDepositInput(),
        UInt64.ZERO,
        UInt64.valueOf(123)
    );

    return depositData;
  }

  @Test
  public void depositDataTest() {
    DepositData expected = createDepositData();
    BytesValue encoded = sszSerializer.encode2(expected);
    DepositData reconstructed = (DepositData) sszSerializer.decode(encoded, DepositData.class);
    assertEquals(expected, reconstructed);
  }

  private Deposit createDeposit1() {
    Deposit deposit = new Deposit(
        new Hash32[0],
        UInt64.ZERO,
        createDepositData()
    );

    return deposit;
  }

  private Deposit createDeposit2() {
    Hash32[] hashes = new Hash32[2];
    hashes[0] = Hashes.keccak256(BytesValue.fromHexString("aa"));
    hashes[1] = Hashes.keccak256(BytesValue.fromHexString("bb"));
    Deposit deposit = new Deposit(
        hashes,
        UInt64.ZERO,
        createDepositData()
    );

    return deposit;
  }

  @Test
  public void depositTest() {
    Deposit expected1 = createDeposit1();
    Deposit expected2 = createDeposit2();
    BytesValue encoded1 = sszSerializer.encode2(expected1);
    BytesValue encoded2 = sszSerializer.encode2(expected2);
    Deposit reconstructed1 = (Deposit) sszSerializer.decode(encoded1, Deposit.class);
    Deposit reconstructed2 = (Deposit) sszSerializer.decode(encoded2, Deposit.class);
    assertEquals(expected1, reconstructed1);
    assertEquals(expected2, reconstructed2);
  }

  private Exit createExit() {
    Exit exit = new Exit(
        UInt64.valueOf(123),
        UInt24.MAX_VALUE,
        Bytes96.fromHexString("aa")
    );

    return exit;
  }

  @Test
  public void exitTest() {
    Exit expected = createExit();
    BytesValue encoded = sszSerializer.encode2(expected);
    Exit reconstructed = (Exit) sszSerializer.decode(encoded, Exit.class);
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
    CustodyChallenge reconstructed = (CustodyChallenge) sszSerializer.decode(encoded, CustodyChallenge.class);
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
    CustodyResponse reconstructed = (CustodyResponse) sszSerializer.decode(encoded, CustodyResponse.class);
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
    CustodyReseed reconstructed = (CustodyReseed) sszSerializer.decode(encoded, CustodyReseed.class);
    assertEquals(expected, reconstructed);
  }

  private ProposalSignedData createProposalSignedData() {
    ProposalSignedData proposalSignedData = new ProposalSignedData(
        UInt64.valueOf(12),
        UInt64.ZERO,
        Hashes.keccak256(BytesValue.fromHexString("aa"))
    );
    return proposalSignedData;
  }

  @Test
  public void proposalSignedDataTest() {
    ProposalSignedData expected = createProposalSignedData();
    BytesValue encoded = sszSerializer.encode2(expected);
    ProposalSignedData reconstructed = (ProposalSignedData) sszSerializer.decode(encoded, ProposalSignedData.class);
    assertEquals(expected, reconstructed);
  }

  private ProposerSlashing createProposerSlashing() {
    ProposerSlashing proposerSlashing = new ProposerSlashing(
        UInt24.MAX_VALUE,
        createProposalSignedData(),
        Bytes96.fromHexString("aa"),
        createProposalSignedData(),
        Bytes96.fromHexString("bb")
    );

    return proposerSlashing;
  }

  @Test
  public void proposerSlashingTest() {
    ProposerSlashing expected = createProposerSlashing();
    BytesValue encoded = sszSerializer.encode2(expected);
    ProposerSlashing reconstructed = (ProposerSlashing) sszSerializer.decode(encoded, ProposerSlashing.class);
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
    BeaconBlockBody beaconBlockBody = new BeaconBlockBody(
        proposerSlashings,
        casperSlashings,
        attestations,
        pocSeedChanges,
        pocChallenges,
        pocResponses,
        deposits,
        exits
    );

    return beaconBlockBody;
  }

  @Test
  public void beaconBlockBodyTest() {
    BeaconBlockBody expected = createBeaconBlockBody();
    BytesValue encoded = sszSerializer.encode2(expected);
    BeaconBlockBody reconstructed = (BeaconBlockBody) sszSerializer.decode(encoded, BeaconBlockBody.class);
    assertEquals(expected, reconstructed);
  }

  private BeaconBlock createBeaconBlock() {
    BeaconBlock beaconBlock = new BeaconBlock(
        UInt64.MAX_VALUE,
        Hashes.keccak256(BytesValue.fromHexString("aa")),
        Hashes.keccak256(BytesValue.fromHexString("bb")),
        Hashes.keccak256(BytesValue.fromHexString("cc")),
        Hashes.keccak256(BytesValue.fromHexString("dd")),
        Bytes96.fromHexString("aa"),
        createBeaconBlockBody()
    );

    return beaconBlock;
  }

  @Test
  public void beaconBlockTest() {
    BeaconBlock expected = createBeaconBlock();
    BytesValue encoded = sszSerializer.encode2(expected);
    BeaconBlock reconstructed = (BeaconBlock) sszSerializer.decode(encoded, BeaconBlock.class);
    assertEquals(expected, reconstructed);
  }

  private BeaconState createBeaconState() {
    BeaconState beaconState = BeaconState.EMPTY;

    return beaconState;
  }

  @Test
  public void beaconStateTest() {
    BeaconState expected = createBeaconState();
    BytesValue encoded = sszSerializer.encode2(expected);
    BeaconState reconstructed = (BeaconState) sszSerializer.decode(encoded, BeaconState.class);
    assertEquals(expected, reconstructed);
  }
}

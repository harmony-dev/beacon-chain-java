package org.ethereum.beacon.validator.proposer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.crypto.BLS381MessageSigner;
import org.ethereum.beacon.validator.crypto.InsecureBLS381MessageSigner;
import org.mockito.Mockito;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconChainProposerTestUtil {

  public static BLS381MessageSigner createRandomSigner() {
    Bytes32 privateKey = BLS381.KeyPair.generate().getPrivate().getEncodedBytes();
    return new InsecureBLS381MessageSigner(privateKey);
  }

  public static PendingOperations createEmptyPendingOperations() {
    return mockPendingOperations(
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  public static List<Attestation> createRandomAttestationList(Random random, int maxCount) {
    return Stream.generate(() -> createRandomAttestation(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static List<ProposerSlashing> createRandomProposerSlashingList(
      Random random, int maxCount) {
    return Stream.generate(() -> createRandomProposerSlashing(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static List<CasperSlashing> createRandomCasperSlashingList(Random random, int maxCount) {
    return Stream.generate(() -> createRandomCasperSlashing(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static List<Exit> createRandomExitList(Random random, int maxCount) {
    return Stream.generate(() -> createRandomExit(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static List<DepositInfo> createRandomDepositList(
      Random random, SpecHelpers specHelpers, UInt64 startIndex, int maxCount) {
    List<DepositInfo> deposits = new ArrayList<>();
    int count = Math.abs(random.nextInt()) % maxCount + 1;
    for (int i = 0; i < count; i++) {
      deposits.add(createRandomDeposit(random, specHelpers, startIndex.plus(i)));
    }
    return deposits;
  }

  public static Attestation createRandomAttestation(Random random) {
    return new Attestation(
        createRandomAttestationData(random),
        BytesValue.wrap(new byte[64]),
        BytesValue.wrap(new byte[64]),
        Bytes96.random(random));
  }

  public static AttestationData createRandomAttestationData(Random random) {
    return new AttestationData(
        UInt64.ZERO,
        ChainSpec.BEACON_CHAIN_SHARD_NUMBER,
        Hash32.random(random),
        Hash32.random(random),
        Hash32.random(random),
        Hash32.random(random),
        UInt64.ZERO,
        Hash32.random(random));
  }

  public static ProposerSlashing createRandomProposerSlashing(Random random) {
    ProposalSignedData signedData1 =
        new ProposalSignedData(
            UInt64.ZERO, ChainSpec.BEACON_CHAIN_SHARD_NUMBER, Hash32.random(random));
    ProposalSignedData signedData2 =
        new ProposalSignedData(
            UInt64.ZERO, ChainSpec.BEACON_CHAIN_SHARD_NUMBER, Hash32.random(random));
    return new ProposerSlashing(
        UInt24.ZERO, signedData1, Bytes96.random(random), signedData2, Bytes96.random(random));
  }

  public static CasperSlashing createRandomCasperSlashing(Random random) {
    SlashableVoteData voteData1 =
        new SlashableVoteData(
            new UInt24[0],
            new UInt24[0],
            createRandomAttestationData(random),
            Bytes96.random(random));
    SlashableVoteData voteData2 =
        new SlashableVoteData(
            new UInt24[0],
            new UInt24[0],
            createRandomAttestationData(random),
            Bytes96.random(random));
    return new CasperSlashing(voteData1, voteData2);
  }

  public static Exit createRandomExit(Random random) {
    return new Exit(UInt64.ZERO, UInt24.ZERO, Bytes96.random(random));
  }

  public static DepositInfo createRandomDeposit(
      Random random, SpecHelpers specHelpers, UInt64 depositIndex) {
    DepositInput depositInput =
        new DepositInput(Bytes48.random(random), Hash32.random(random), Bytes96.random(random));

    DepositData depositData =
        new DepositData(
            depositInput,
            specHelpers.getChainSpec().getMaxDeposit().toGWei(),
            UInt64.valueOf(System.currentTimeMillis() / 1000));

    List<Hash32> merkleBranch =
        Collections.nCopies(
            specHelpers.getChainSpec().getDepositContractTreeDepth().getIntValue(), Hash32.ZERO);
    Deposit deposit = new Deposit(merkleBranch, depositIndex, depositData);
    return new DepositInfo(deposit, createRandomEth1Data(random));
  }

  public static PendingOperations mockPendingOperations(
      List<Attestation> attestations,
      List<ProposerSlashing> proposerSlashings,
      List<CasperSlashing> casperSlashings,
      List<Exit> exits) {
    PendingOperations pendingOperations = Mockito.mock(PendingOperations.class);
    when(pendingOperations.getAttestations()).thenReturn(attestations);
    when(pendingOperations.peekProposerSlashings(anyInt())).thenReturn(proposerSlashings);
    when(pendingOperations.peekCasperSlashings(anyInt())).thenReturn(casperSlashings);
    when(pendingOperations.peekAggregatedAttestations(anyInt(), any())).thenReturn(attestations);
    when(pendingOperations.peekExits(anyInt())).thenReturn(exits);
    return pendingOperations;
  }

  public static ObservableBeaconState createInitialState(Random random, SpecHelpers specHelpers) {
    return createInitialState(random, specHelpers, createEmptyPendingOperations());
  }

  public static ObservableBeaconState createInitialState(
      Random random, SpecHelpers specHelpers, PendingOperations operations) {
    BeaconBlock genesis = BeaconBlocks.createGenesis(specHelpers.getChainSpec());
    ChainStart chainStart =
        new ChainStart(
            UInt64.ZERO,
            new Eth1Data(Hash32.random(random), Hash32.random(random)),
            Collections.emptyList());
    InitialStateTransition stateTransition = new InitialStateTransition(chainStart, specHelpers);

    BeaconState state = stateTransition.apply(genesis).getCanonicalState();
    return new ObservableBeaconState(genesis, state, operations);
  }

  public static StateTransition<BeaconState> createDummyStateTransition() {
    return (block, state) -> state.createMutableCopy().withSlot(block.getSlot());
  }

  public static Eth1Data createRandomEth1Data(Random random) {
    return new Eth1Data(Hash32.random(random), Hash32.random(random));
  }

  public static DepositContract mockDepositContract(Random random, List<DepositInfo> deposits) {
    return mockDepositContract(deposits, createRandomEth1Data(random));
  }

  public static DepositContract mockDepositContract(List<DepositInfo> deposits, Eth1Data eth1Data) {
    DepositContract depositContract = Mockito.mock(DepositContract.class);
    when(depositContract.getLatestEth1Data()).thenReturn(Optional.of(eth1Data));
    when(depositContract.peekDeposits(anyInt(), any(), any())).thenReturn(deposits);
    when(depositContract.hasDepositRoot(any(), any())).thenReturn(true);
    return depositContract;
  }

  public static BeaconChainProposer createProposer(
      StateTransition<BeaconState> stateStateTransition,
      DepositContract depositContract,
      SpecHelpers specHelpers) {
    return new BeaconChainProposerImpl(
        specHelpers, specHelpers.getChainSpec(), stateStateTransition, depositContract);
  }
}

package org.ethereum.beacon.validator.proposer;

import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.mockProposer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.chain.util.ObservableBeaconStateTestUtil;
import org.ethereum.beacon.chain.util.PendingOperationsTestUtil;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.util.AttestationTestUtil;
import org.ethereum.beacon.core.util.AttesterSlashingTestUtil;
import org.ethereum.beacon.core.util.DepositTestUtil;
import org.ethereum.beacon.core.util.Eth1DataTestUtil;
import org.ethereum.beacon.core.util.ExitTestUtil;
import org.ethereum.beacon.core.util.ProposerSlashingTestUtil;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.pow.util.DepositContractTestUtil;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.validator.util.MessageSignerTestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconChainProposerTest {

  @Test
  public void proposeABlock() {
    Random random = new Random();

    SpecHelpers specHelpers = new SpecHelpers(ChainSpec.DEFAULT);
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    StateTransition<BeaconState> stateTransition =
        StateTransitionTestUtil.createSlotFromBlockTransition();
    BeaconChainProposer proposer = mockProposer(stateTransition, depositContract, specHelpers);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers);
    BeaconState initialState = initialObservedState.getLatestSlotState();
    BeaconBlock block = proposer.propose(initialObservedState, signer);

    BeaconState stateAfterBlock = stateTransition.apply(block, initialState);

    Assert.assertEquals(specHelpers.hash_tree_root(stateAfterBlock), block.getStateRoot());
    Assert.assertTrue(verifySignature(specHelpers, initialState, block, signer));
  }

  @Test
  public void proposeABlockWithOperations() {
    Random random = new Random();

    SpecHelpers specHelpers = new SpecHelpers(ChainSpec.DEFAULT);
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    StateTransition<BeaconState> stateTransition =
        StateTransitionTestUtil.createSlotFromBlockTransition();
    BeaconChainProposer proposer = mockProposer(stateTransition, depositContract, specHelpers);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    List<Attestation> attestations =
        AttestationTestUtil.createRandomList(
            random, specHelpers.getChainSpec().getMaxAttestations());
    List<ProposerSlashing> proposerSlashings =
        ProposerSlashingTestUtil.createRandomList(
            random, specHelpers.getChainSpec().getMaxProposerSlashings());
    List<AttesterSlashing> casperSlashings =
        AttesterSlashingTestUtil.createRandomList(
            random, specHelpers.getChainSpec().getMaxAttesterSlashings());
    List<Exit> exits =
        ExitTestUtil.createRandomList(random, specHelpers.getChainSpec().getMaxExits());

    PendingOperations pendingOperations =
        PendingOperationsTestUtil.mockPendingOperations(
            attestations, attestations, proposerSlashings, casperSlashings, exits);
    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers, pendingOperations);
    BeaconState initialState = initialObservedState.getLatestSlotState();
    BeaconBlock block = proposer.propose(initialObservedState, signer);

    Mockito.verify(pendingOperations)
        .peekAggregatedAttestations(
            specHelpers.getChainSpec().getMaxAttestations(),
            initialState
                .getSlot()
                .plus(specHelpers.getChainSpec().getMinAttestationInclusionDelay()));

    Mockito.verify(pendingOperations)
        .peekProposerSlashings(specHelpers.getChainSpec().getMaxProposerSlashings());
    Mockito.verify(pendingOperations)
        .peekAttesterSlashings(specHelpers.getChainSpec().getMaxAttesterSlashings());
    Mockito.verify(pendingOperations).peekExits(specHelpers.getChainSpec().getMaxExits());

    BeaconState stateAfterBlock = stateTransition.apply(block, initialState);

    Assert.assertEquals(specHelpers.hash_tree_root(stateAfterBlock), block.getStateRoot());
    Assert.assertTrue(verifySignature(specHelpers, initialState, block, signer));

    Assert.assertEquals(attestations, block.getBody().getAttestations());
    Assert.assertEquals(proposerSlashings, block.getBody().getProposerSlashings());
    Assert.assertEquals(casperSlashings, block.getBody().getAttesterSlashings());
    Assert.assertEquals(exits, block.getBody().getExits());
  }

  @Test
  public void proposeABlockWithDeposits() {
    Random random = new Random();

    SpecHelpers specHelpers = new SpecHelpers(ChainSpec.DEFAULT);

    List<Deposit> deposits =
        DepositTestUtil.createRandomList(
            random,
            specHelpers.getChainSpec(),
            UInt64.ZERO,
            specHelpers.getChainSpec().getMaxDeposits());
    Eth1Data eth1Data = Eth1DataTestUtil.createRandom(random);
    List<DepositInfo> depositInfos =
        deposits.stream()
            .map(deposit -> new DepositInfo(deposit, eth1Data))
            .collect(Collectors.toList());
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, depositInfos);
    StateTransition<BeaconState> stateTransition =
        StateTransitionTestUtil.createSlotFromBlockTransition();
    BeaconChainProposer proposer = mockProposer(stateTransition, depositContract, specHelpers);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(
            random, specHelpers, PendingOperationsTestUtil.createEmptyPendingOperations());
    BeaconState initialState = initialObservedState.getLatestSlotState();
    BeaconBlock block = proposer.propose(initialObservedState, signer);

    Mockito.verify(depositContract)
        .peekDeposits(
            Mockito.eq(specHelpers.getChainSpec().getMaxDeposits()),
            Mockito.any(),
            Mockito.eq(initialState.getLatestEth1Data()));

    BeaconState stateAfterBlock = stateTransition.apply(block, initialState);

    Assert.assertEquals(specHelpers.hash_tree_root(stateAfterBlock), block.getStateRoot());
    Assert.assertTrue(verifySignature(specHelpers, initialState, block, signer));

    Assert.assertEquals(
        depositInfos.stream().map(DepositInfo::getDeposit).collect(Collectors.toList()),
        block.getBody().getDeposits());
    Assert.assertEquals(depositContract.getLatestEth1Data(), Optional.of(block.getEth1Data()));
  }

  private boolean verifySignature(
      SpecHelpers specHelpers,
      BeaconState initialState,
      BeaconBlock block,
      MessageSigner<BLSSignature> signer) {

    ProposalSignedData signedData =
        new ProposalSignedData(
            initialState.getSlot(),
            specHelpers.getChainSpec().getBeaconChainShardNumber(),
            specHelpers.hash_tree_root(block));
    BLSSignature expectedSignature =
        signer.sign(
            specHelpers.hash_tree_root(signedData),
            specHelpers.get_domain(
                initialState.getForkData(),
                specHelpers.get_current_epoch(initialState),
                SignatureDomains.PROPOSAL));

    return expectedSignature.equals(block.getSignature());
  }
}

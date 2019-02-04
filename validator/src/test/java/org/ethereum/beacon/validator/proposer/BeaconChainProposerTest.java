package org.ethereum.beacon.validator.proposer;

import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createDummyStateTransition;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createEmptyPendingOperations;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createInitialState;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createProposer;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createRandomAttestationList;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createRandomCasperSlashingList;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createRandomDepositList;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createRandomExitList;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createRandomProposerSlashingList;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.createRandomSigner;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.mockDepositContract;
import static org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil.mockPendingOperations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconChainProposerTest {

  @Test
  public void proposeABlock() {
    Random random = new Random();

    SpecHelpers specHelpers = new SpecHelpers(ChainSpec.DEFAULT);
    DepositContract depositContract = mockDepositContract(random, Collections.emptyList());
    StateTransition<BeaconState> stateTransition = createDummyStateTransition();
    BeaconChainProposer proposer = createProposer(stateTransition, depositContract, specHelpers);
    MessageSigner<Bytes96> signer = createRandomSigner();

    ObservableBeaconState initialObservedState = createInitialState(random, specHelpers);
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
    DepositContract depositContract = mockDepositContract(random, Collections.emptyList());
    StateTransition<BeaconState> stateTransition = createDummyStateTransition();
    BeaconChainProposer proposer = createProposer(stateTransition, depositContract, specHelpers);
    MessageSigner<Bytes96> signer = createRandomSigner();

    List<Attestation> attestations =
        createRandomAttestationList(random, specHelpers.getChainSpec().getMaxAttestations());
    List<ProposerSlashing> proposerSlashings =
        createRandomProposerSlashingList(
            random, specHelpers.getChainSpec().getMaxProposerSlashings());
    List<CasperSlashing> casperSlashings =
        createRandomCasperSlashingList(random, specHelpers.getChainSpec().getMaxCasperSlashings());
    List<Exit> exits = createRandomExitList(random, specHelpers.getChainSpec().getMaxExits());

    PendingOperations pendingOperations =
        mockPendingOperations(attestations, proposerSlashings, casperSlashings, exits);
    ObservableBeaconState initialObservedState =
        createInitialState(random, specHelpers, pendingOperations);
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
        .peekCasperSlashings(specHelpers.getChainSpec().getMaxCasperSlashings());
    Mockito.verify(pendingOperations).peekExits(specHelpers.getChainSpec().getMaxExits());

    BeaconState stateAfterBlock = stateTransition.apply(block, initialState);

    Assert.assertEquals(specHelpers.hash_tree_root(stateAfterBlock), block.getStateRoot());
    Assert.assertTrue(verifySignature(specHelpers, initialState, block, signer));

    Assert.assertEquals(attestations, block.getBody().getAttestations());
    Assert.assertEquals(proposerSlashings, block.getBody().getProposerSlashings());
    Assert.assertEquals(casperSlashings, block.getBody().getCasperSlashings());
    Assert.assertEquals(exits, block.getBody().getExits());
  }

  @Test
  public void proposeABlockWithDeposits() {
    Random random = new Random();

    SpecHelpers specHelpers = new SpecHelpers(ChainSpec.DEFAULT);

    List<DepositInfo> deposits =
        createRandomDepositList(
            random, specHelpers, UInt64.ZERO, specHelpers.getChainSpec().getMaxDeposits());
    DepositContract depositContract = mockDepositContract(random, deposits);
    StateTransition<BeaconState> stateTransition = createDummyStateTransition();
    BeaconChainProposer proposer = createProposer(stateTransition, depositContract, specHelpers);
    MessageSigner<Bytes96> signer = createRandomSigner();

    ObservableBeaconState initialObservedState =
        createInitialState(random, specHelpers, createEmptyPendingOperations());
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
        deposits.stream().map(DepositInfo::getDeposit).collect(Collectors.toList()),
        block.getBody().getDeposits());
    Assert.assertEquals(depositContract.getLatestEth1Data(), Optional.of(block.getEth1Data()));
  }

  private boolean verifySignature(
      SpecHelpers specHelpers,
      BeaconState initialState,
      BeaconBlock block,
      MessageSigner<Bytes96> signer) {

    ProposalSignedData signedData =
        new ProposalSignedData(
            initialState.getSlot(),
            specHelpers.getChainSpec().getBeaconChainShardNumber(),
            specHelpers.hash_tree_root(block));
    Bytes96 expectedSignature =
        signer.sign(
            specHelpers.hash_tree_root(signedData),
            specHelpers.get_domain(
                initialState.getForkData(), initialState.getSlot(), SignatureDomains.PROPOSAL));

    return expectedSignature.equals(block.getSignature());
  }
}

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
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
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

    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();
    BeaconChainProposer proposer =
        mockProposer(perBlockTransition, depositContract, spec);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec);
    BeaconState initialState = initialObservedState.getLatestSlotState();
    BeaconBlock block = proposer.propose(initialObservedState, signer);

    BeaconStateEx stateAfterBlock =
        perBlockTransition.apply(new BeaconStateExImpl(initialState), block);

    Assert.assertEquals(
        spec.hash_tree_root(stateAfterBlock), block.getStateRoot());
    Assert.assertTrue(verifySignature(spec, initialState, block, signer));
  }

  @Test
  public void proposeABlockWithOperations() {
    Random random = new Random();

    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();
    BeaconChainProposer proposer =
        mockProposer(perBlockTransition, depositContract, spec);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    List<Attestation> attestations =
        AttestationTestUtil.createRandomList(
            random, spec.getConstants().getMaxAttestations());
    List<ProposerSlashing> proposerSlashings =
        ProposerSlashingTestUtil.createRandomList(
            random, spec.getConstants().getMaxProposerSlashings());
    List<AttesterSlashing> casperSlashings =
        AttesterSlashingTestUtil.createRandomList(
            random, spec.getConstants().getMaxAttesterSlashings());
    List<VoluntaryExit> voluntaryExits =
        ExitTestUtil.createRandomList(random, spec.getConstants().getMaxVoluntaryExits());

    PendingOperations pendingOperations =
        PendingOperationsTestUtil.mockPendingOperations(
            attestations, attestations, proposerSlashings, casperSlashings, voluntaryExits);
    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec, pendingOperations);
    BeaconState initialState = initialObservedState.getLatestSlotState();
    BeaconBlock block = proposer.propose(initialObservedState, signer);

    Mockito.verify(pendingOperations)
        .peekAggregateAttestations(spec.getConstants().getMaxAttestations());

    Mockito.verify(pendingOperations)
        .peekProposerSlashings(spec.getConstants().getMaxProposerSlashings());
    Mockito.verify(pendingOperations)
        .peekAttesterSlashings(spec.getConstants().getMaxAttesterSlashings());
    Mockito.verify(pendingOperations).peekExits(spec.getConstants().getMaxVoluntaryExits());

    BeaconStateEx stateAfterBlock =
        perBlockTransition.apply(new BeaconStateExImpl(initialState), block);

    Assert.assertEquals(
        spec.hash_tree_root(stateAfterBlock), block.getStateRoot());
    Assert.assertTrue(verifySignature(spec, initialState, block, signer));

    Assert.assertEquals(attestations, block.getBody().getAttestations().listCopy());
    Assert.assertEquals(proposerSlashings, block.getBody().getProposerSlashings().listCopy());
    Assert.assertEquals(casperSlashings, block.getBody().getAttesterSlashings().listCopy());
    Assert.assertEquals(voluntaryExits, block.getBody().getVoluntaryExits().listCopy());
  }

  @Test
  public void proposeABlockWithDeposits() {
    Random random = new Random();

    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();

    List<Deposit> deposits =
        DepositTestUtil.createRandomList(
            random,
            spec.getConstants(),
            UInt64.ZERO,
            spec.getConstants().getMaxDeposits());
    Eth1Data eth1Data = Eth1DataTestUtil.createRandom(random);
    List<DepositInfo> depositInfos =
        deposits.stream()
            .map(deposit -> new DepositInfo(deposit, eth1Data))
            .collect(Collectors.toList());
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, depositInfos);
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();
    BeaconChainProposer proposer =
        mockProposer(perBlockTransition, depositContract, spec);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(
            random, spec, PendingOperationsTestUtil.createEmptyPendingOperations());
    BeaconState initialState = initialObservedState.getLatestSlotState();
    BeaconBlock block = proposer.propose(initialObservedState, signer);

    Mockito.verify(depositContract)
        .peekDeposits(
            Mockito.eq(spec.getConstants().getMaxDeposits()),
            Mockito.any(),
            Mockito.eq(initialState.getLatestEth1Data()));

    BeaconStateEx stateAfterBlock =
        perBlockTransition.apply(new BeaconStateExImpl(initialState), block);

    Assert.assertEquals(
        spec.hash_tree_root(stateAfterBlock), block.getStateRoot());
    Assert.assertTrue(verifySignature(spec, initialState, block, signer));

    Assert.assertEquals(
        depositInfos.stream().map(DepositInfo::getDeposit).collect(Collectors.toList()),
        block.getBody().getDeposits().listCopy());
    Assert.assertEquals(depositContract.getLatestEth1Data(), Optional.of(block.getBody().getEth1Data()));
  }

  private boolean verifySignature(
      BeaconChainSpec spec,
      BeaconState initialState,
      BeaconBlock block,
      MessageSigner<BLSSignature> signer) {

    BLSSignature expectedSignature =
        signer.sign(
            spec.signing_root(block),
            spec.get_domain(initialState, SignatureDomains.BEACON_PROPOSER));

    return expectedSignature.equals(block.getSignature());
  }
}

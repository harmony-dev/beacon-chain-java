package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.util.ObservableBeaconStateTestUtil;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.util.DepositContractTestUtil;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Random;

import static org.ethereum.beacon.validator.ValidatorSpecTestUtil.verifySignature;

public class BeaconProposerSpecTest {

  @Test
  public void proposeABlock() {
    Random random = new Random();

    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();
    BeaconProposerSpec proposerSpec = Mockito.spy(new BeaconProposerSpec(spec));
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();

    ObservableBeaconState initialObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec);
    BeaconState initialState = initialObservedState.getLatestSlotState();

    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();
    BLSSignature randaoReveal = proposerSpec.getRandaoReveal(initialState, signer);
    BeaconBlock.Builder builder =
        proposerSpec.prepareBuilder(
            perBlockTransition,
            depositContract,
            initialState.getSlot(),
            randaoReveal,
            initialObservedState);
    BLSSignature signature =
        proposerSpec.getProposalSignature(initialState, builder.build(), signer);
    builder.withSignature(signature);
    BeaconBlock block = builder.build();

    BeaconStateEx stateAfterBlock =
        perBlockTransition.apply(new BeaconStateExImpl(initialState), block);

    Assert.assertEquals(spec.hash_tree_root(stateAfterBlock), block.getStateRoot());
    Assert.assertTrue(verifySignature(spec, initialState, block, signer));
  }
}

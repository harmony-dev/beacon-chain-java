package org.ethereum.beacon.chain;

import java.util.Collections;
import java.util.stream.IntStream;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateEx;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Millis;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class DefaultBeaconChainTest {

  @Test
  public void insertAChain() {
    SpecHelpers specHelpers = SpecHelpers.createDefault();
    StateTransition<BeaconStateEx> perSlotTransition =
        StateTransitionTestUtil.createNextSlotTransition();
    MutableBeaconChain beaconChain = createBeaconChain(specHelpers, perSlotTransition);

    beaconChain.init();
    BeaconTuple initialTuple = beaconChain.getRecentlyProcessed();
    Assert.assertEquals(
        specHelpers.getChainSpec().getGenesisSlot(), initialTuple.getBlock().getSlot());

    IntStream.range(0, 10)
        .forEach(
            (idx) -> {
              BeaconTuple recentlyProcessed = beaconChain.getRecentlyProcessed();
              BeaconBlock aBlock = createBlock(recentlyProcessed, specHelpers, perSlotTransition);
              Assert.assertTrue(beaconChain.insert(aBlock));
              Assert.assertEquals(aBlock, beaconChain.getRecentlyProcessed().getBlock());

              System.out.println("Inserted block: " + (idx + 1));
            });
  }

  private BeaconBlock createBlock(
      BeaconTuple parent,
      SpecHelpers specHelpers,
      StateTransition<BeaconStateEx> perSlotTransition) {
    BeaconBlock block =
        new BeaconBlock(
            specHelpers.get_current_slot(parent.getState()),
            specHelpers.hash_tree_root(parent.getBlock()),
            Hash32.ZERO,
            specHelpers.getChainSpec().getEmptySignature(),
            Eth1Data.EMPTY,
            specHelpers.getChainSpec().getEmptySignature(),
            BeaconBlockBody.EMPTY);
    BeaconState state =
        perSlotTransition
            .apply(
                new BeaconStateEx(parent.getState(), specHelpers.hash_tree_root(parent.getBlock())))
            .getCanonicalState();

    return block.withStateRoot(specHelpers.hash_tree_root(state));
  }

  private MutableBeaconChain createBeaconChain(
      SpecHelpers specHelpers, StateTransition<BeaconStateEx> perSlotTransition) {
    Time start =
        Millis.of(System.currentTimeMillis())
            .getSeconds();
//            .minus(
//                specHelpers
//                    .getChainSpec()
//                    .getSlotDuration()
//                    .times(specHelpers.getChainSpec().getEpochLength()));
    ChainStart chainStart = new ChainStart(start, Eth1Data.EMPTY, Collections.emptyList());
    BlockTransition<BeaconStateEx> initialTransition =
        new InitialStateTransition(chainStart, specHelpers);
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();
    StateTransition<BeaconStateEx> perEpochTransition =
        StateTransitionTestUtil.createStateWithNoTransition();
    BeaconBlockVerifier blockVerifier = (block, state) -> VerificationResult.PASSED;
    BeaconStateVerifier stateVerifier = (block, state) -> VerificationResult.PASSED;
    Database database = Database.inMemoryDB();
    BeaconChainStorage chainStorage = BeaconChainStorageFactory.get().create(database);

    return new DefaultBeaconChain(
        specHelpers,
        initialTransition,
        perSlotTransition,
        perBlockTransition,
        perEpochTransition,
        blockVerifier,
        stateVerifier,
        chainStorage);
  }
}

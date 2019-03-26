package org.ethereum.beacon.chain;

import java.util.Collections;
import java.util.stream.IntStream;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.StateCachingTransition;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class DefaultBeaconChainTest {

  @Test
  public void insertAChain() {
    Schedulers schedulers = Schedulers.createDefault();

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(SpecConstants.DEFAULT);
    StateTransition<BeaconStateEx> perSlotTransition =
        StateTransitionTestUtil.createNextSlotTransition();
    MutableBeaconChain beaconChain = createBeaconChain(specHelpers, perSlotTransition, schedulers);

    beaconChain.init();
    BeaconTuple initialTuple = beaconChain.getRecentlyProcessed();
    Assert.assertEquals(
        specHelpers.getConstants().getGenesisSlot(), initialTuple.getBlock().getSlot());

    IntStream.range(0, 10)
        .forEach(
            (idx) -> {
              BeaconTuple recentlyProcessed = beaconChain.getRecentlyProcessed();
              BeaconBlock aBlock = createBlock(recentlyProcessed, specHelpers,
                  schedulers.getCurrentTime(), perSlotTransition);
              Assert.assertTrue(beaconChain.insert(aBlock));
              Assert.assertEquals(aBlock, beaconChain.getRecentlyProcessed().getBlock());

              System.out.println("Inserted block: " + (idx + 1));
            });
  }

  private BeaconBlock createBlock(
      BeaconTuple parent,
      SpecHelpers specHelpers, long currentTime,
      StateTransition<BeaconStateEx> perSlotTransition) {
    BeaconBlock block =
        new BeaconBlock(
            specHelpers.get_current_slot(parent.getState(), currentTime),
            specHelpers.signed_root(parent.getBlock()),
            Hash32.ZERO,
            BeaconBlockBody.EMPTY,
            specHelpers.getConstants().getEmptySignature());
    BeaconState state =
        perSlotTransition
            .apply(
                new BeaconStateExImpl(parent.getState(), specHelpers.hash_tree_root(parent.getBlock())));

    return block.withStateRoot(specHelpers.hash_tree_root(state));
  }

  private MutableBeaconChain createBeaconChain(
      SpecHelpers specHelpers, StateTransition<BeaconStateEx> perSlotTransition, Schedulers schedulers) {
    Time start = Time.castFrom(UInt64.valueOf(schedulers.getCurrentTime() / 1000));
    ChainStart chainStart = new ChainStart(start, Eth1Data.EMPTY, Collections.emptyList());
    BlockTransition<BeaconStateEx> initialTransition =
        new InitialStateTransition(chainStart, specHelpers);
    StateTransition<BeaconStateEx> stateCaching =
        StateTransitionTestUtil.createStateWithNoTransition();
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
        new ExtendedSlotTransition(stateCaching, new PerEpochTransition(specHelpers) {
          @Override
          public BeaconStateEx apply(BeaconStateEx stateEx) {
            return perEpochTransition.apply(stateEx);
          }
        }, perSlotTransition, specHelpers),
        perBlockTransition,
        blockVerifier,
        stateVerifier,
        chainStorage,
        schedulers);
  }
}

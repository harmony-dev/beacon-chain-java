package org.ethereum.beacon.chain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateEx;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.db.Database;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class DefaultBeaconChain implements MutableBeaconChain {

  private final SpecHelpers specHelpers;
  private final BlockTransition<BeaconStateEx> initialTransition;
  private final StateTransition<BeaconStateEx> perSlotTransition;
  private final BlockTransition<BeaconStateEx> perBlockTransition;
  private final StateTransition<BeaconStateEx> perEpochTransition;
  private final BeaconBlockVerifier blockVerifier;
  private final BeaconStateVerifier stateVerifier;

  private final BeaconChainStorage chainStorage;
  private final BeaconTupleStorage tupleStorage;
  private final Database database;

  private final ReplayProcessor<BeaconTuple> blockSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconTuple> blockStream =
      Flux.from(blockSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("DefaultBeaconChain.block");

  private BeaconTuple recentlyProcessed;

  public DefaultBeaconChain(
      SpecHelpers specHelpers,
      BlockTransition<BeaconStateEx> initialTransition,
      StateTransition<BeaconStateEx> perSlotTransition,
      BlockTransition<BeaconStateEx> perBlockTransition,
      StateTransition<BeaconStateEx> perEpochTransition,
      BeaconBlockVerifier blockVerifier,
      BeaconStateVerifier stateVerifier,
      BeaconChainStorage chainStorage,
      Database database) {
    this.specHelpers = specHelpers;
    this.initialTransition = initialTransition;
    this.perSlotTransition = perSlotTransition;
    this.perBlockTransition = perBlockTransition;
    this.perEpochTransition = perEpochTransition;
    this.blockVerifier = blockVerifier;
    this.stateVerifier = stateVerifier;
    this.chainStorage = chainStorage;
    this.tupleStorage = chainStorage.getTupleStorage();
    this.database = database;
  }

  @Override
  public void init() {
    if (tupleStorage.isEmpty()) {
      initializeStorage();
    }
    this.recentlyProcessed = fetchRecentTuple();
  }

  private BeaconTuple fetchRecentTuple() {
    SlotNumber maxSlot = chainStorage.getBlockStorage().getMaxSlot();
    List<Hash32> latestBlockRoots = chainStorage.getBlockStorage().getSlotBlocks(maxSlot);
    assert latestBlockRoots.size() > 0;
    return tupleStorage
        .get(latestBlockRoots.get(0))
        .orElseThrow(
            () -> new RuntimeException("Block with stored maxSlot not found, maxSlot: " + maxSlot));
  }

  private void initializeStorage() {
    BeaconBlock initialGenesis = BeaconBlocks.createGenesis(specHelpers.getChainSpec());
    BeaconStateEx initialState =
        initialTransition.apply(
            new BeaconStateEx(BeaconState.getEmpty(), Hash32.ZERO), initialGenesis);

    Hash32 initialStateRoot = specHelpers.hash_tree_root(initialState.getCanonicalState());
    BeaconBlock genesis = initialGenesis.withStateRoot(initialStateRoot);
    Hash32 genesisRoot = specHelpers.hash_tree_root(genesis);
    BeaconTuple tuple = BeaconTuple.of(genesis, initialState);

    tupleStorage.put(tuple);
    chainStorage.getJustifiedStorage().set(genesisRoot);
    chainStorage.getFinalizedStorage().set(genesisRoot);
  }

  @Override
  public synchronized boolean insert(BeaconBlock block) {
    if (rejectedByTime(block)) {
      return false;
    }

    if (exist(block)) {
      return false;
    }

    if (!hasParent(block)) {
      return false;
    }

    BeaconStateEx parentState = pullParentState(block);

    BeaconStateEx preBlockState = applyEmptySlotTransitions(parentState, block);
    VerificationResult blockVerification =
        blockVerifier.verify(block, preBlockState.getCanonicalState());
    if (!blockVerification.isPassed()) {
      return false;
    }

    BeaconStateEx postBlockState = perBlockTransition.apply(preBlockState, block);
    if (specHelpers.is_epoch_end(block.getSlot())) {
      postBlockState = perEpochTransition.apply(preBlockState);
    }

    VerificationResult stateVerification =
        stateVerifier.verify(postBlockState.getCanonicalState(), block);
    if (stateVerification.isPassed()) {
      return false;
    }

    BeaconTuple newTuple = BeaconTuple.of(block, postBlockState);
    tupleStorage.put(newTuple);
    updateFinality(parentState.getCanonicalState(), postBlockState.getCanonicalState());

    chainStorage.commit();
    database.commit();

    this.recentlyProcessed = newTuple;
    blockSink.onNext(newTuple);

    return true;
  }

  private void updateFinality(BeaconState previous, BeaconState current) {
    if (previous.getFinalizedEpoch().less(current.getFinalizedEpoch())) {
      Hash32 finalizedRoot =
          specHelpers.get_block_root(
              current, specHelpers.get_epoch_end_slot(current.getFinalizedEpoch()));
      chainStorage.getFinalizedStorage().set(finalizedRoot);
    }
    if (previous.getJustifiedEpoch().less(current.getJustifiedEpoch())) {
      Hash32 justifiedRoot =
          specHelpers.get_block_root(
              current, specHelpers.get_epoch_end_slot(current.getJustifiedEpoch()));
      chainStorage.getJustifiedStorage().set(justifiedRoot);
    }
  }

  private BeaconStateEx pullParentState(BeaconBlock block) {
    Optional<BeaconTuple> parent = tupleStorage.get(block.getParentRoot());
    checkArgument(parent.isPresent(), "No parent for block %s", block);
    BeaconTuple parentTuple = parent.get();
    Hash32 parentHash = specHelpers.hash_tree_root(parentTuple.getBlock());

    return new BeaconStateEx(parentTuple.getState(), parentHash);
  }

  private boolean exist(BeaconBlock block) {
    Hash32 blockHash = specHelpers.hash_tree_root(block);
    return tupleStorage.get(blockHash).isPresent();
  }

  private boolean hasParent(BeaconBlock block) {
    return tupleStorage.get(block.getParentRoot()).isPresent();
  }

  /**
   * There is no sense in importing block with a slot number less or equal to latest finalized slot
   * or too far in the future.
   *
   * @param block block to run check on.
   * @return true if block should be rejected, false otherwise.
   */
  private boolean rejectedByTime(BeaconBlock block) {
    SlotNumber finalizedSlot =
        specHelpers.get_epoch_end_slot(recentlyProcessed.getState().getFinalizedEpoch());
    SlotNumber nextToCurrentSlot =
        specHelpers.get_current_slot(recentlyProcessed.getState()).increment();

    return block.getSlot().lessEqual(finalizedSlot) || block.getSlot().greater(nextToCurrentSlot);
  }

  private BeaconStateEx applyEmptySlotTransitions(BeaconStateEx source, BeaconBlock block) {
    BeaconStateEx result = source;
    while (result.getCanonicalState().getSlot().less(block.getSlot())) {
      result = perSlotTransition.apply(result);
      if (specHelpers.is_epoch_end(result.getCanonicalState().getSlot())) {
        result = perEpochTransition.apply(result);
      }
    }

    return perSlotTransition.apply(result);
  }

  @Override
  public Publisher<BeaconTuple> getBlockStatesStream() {
    return blockStream;
  }
}

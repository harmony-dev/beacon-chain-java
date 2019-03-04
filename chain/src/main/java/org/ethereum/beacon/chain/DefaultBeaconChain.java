package org.ethereum.beacon.chain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class DefaultBeaconChain implements MutableBeaconChain {
  private static final Logger logger = LogManager.getLogger(DefaultBeaconChain.class);

  private final SpecHelpers specHelpers;
  private final BlockTransition<BeaconStateEx> initialTransition;
  private final StateTransition<BeaconStateEx> perSlotTransition;
  private final BlockTransition<BeaconStateEx> perBlockTransition;
  private final StateTransition<BeaconStateEx> perEpochTransition;
  private final BeaconBlockVerifier blockVerifier;
  private final BeaconStateVerifier stateVerifier;

  private final BeaconChainStorage chainStorage;
  private final BeaconTupleStorage tupleStorage;

  private final ReplayProcessor<BeaconTuple> blockSink = ReplayProcessor.cacheLast();
  private final Publisher<BeaconTuple> blockStream;
  private final Schedulers schedulers;

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
      Schedulers schedulers) {
    this.specHelpers = specHelpers;
    this.initialTransition = initialTransition;
    this.perSlotTransition = perSlotTransition;
    this.perBlockTransition = perBlockTransition;
    this.perEpochTransition = perEpochTransition;
    this.blockVerifier = blockVerifier;
    this.stateVerifier = stateVerifier;
    this.chainStorage = chainStorage;
    this.tupleStorage = chainStorage.getTupleStorage();
    this.schedulers = schedulers;

    blockStream = Flux.from(blockSink)
            .publishOn(schedulers.reactorEvents())
            .onBackpressureError()
            .name("DefaultBeaconChain.block");
  }

  @Override
  public void init() {
    if (tupleStorage.isEmpty()) {
      initializeStorage();
    }
    this.recentlyProcessed = fetchRecentTuple();
    blockSink.onNext(recentlyProcessed);
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
    BeaconStateEx initialState = initialTransition.apply(BeaconStateEx.getEmpty(), initialGenesis);

    Hash32 initialStateRoot = specHelpers.hash_tree_root(initialState);
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

    BeaconStateEx preBlockState = applyEmptySlotTransitionsTillBlock(parentState, block);
    VerificationResult blockVerification =
        blockVerifier.verify(block, preBlockState);
    if (!blockVerification.isPassed()) {
      logger.warn("Block verification failed: " + blockVerification + ": " +
          block.toString(specHelpers.getChainSpec(), parentState.getGenesisTime(), specHelpers::hash_tree_root));
      return false;
    }

    BeaconStateEx postBlockState = perBlockTransition.apply(preBlockState, block);
    BeaconStateEx postEpochState;
    if (specHelpers.is_epoch_end(block.getSlot())) {
      postEpochState = perEpochTransition.apply(postBlockState);
    } else {
      postEpochState = postBlockState;
    }

    VerificationResult stateVerification =
        stateVerifier.verify(postEpochState, block);
    if (!stateVerification.isPassed()) {
      logger.warn("State verification failed: " + stateVerification);
      return false;
    }

    BeaconTuple newTuple = BeaconTuple.of(block, postEpochState);
    tupleStorage.put(newTuple);
    updateFinality(parentState, postEpochState);

    chainStorage.commit();

    this.recentlyProcessed = newTuple;
    blockSink.onNext(newTuple);

    logger.info(
        "new block inserted: {}",
        newTuple
            .getBlock()
            .toString(
                specHelpers.getChainSpec(),
                newTuple.getState().getGenesisTime(),
                specHelpers::hash_tree_root));

    return true;
  }

  @Override
  public BeaconTuple getRecentlyProcessed() {
    return recentlyProcessed;
  }

  private void updateFinality(BeaconState previous, BeaconState current) {
    if (previous.getFinalizedEpoch().less(current.getFinalizedEpoch())) {
      Hash32 finalizedRoot =
          specHelpers.get_block_root(
              current, specHelpers.get_epoch_start_slot(current.getFinalizedEpoch()));
      chainStorage.getFinalizedStorage().set(finalizedRoot);
    }
    if (previous.getJustifiedEpoch().less(current.getJustifiedEpoch())) {
      Hash32 justifiedRoot =
          specHelpers.get_block_root(
              current, specHelpers.get_epoch_start_slot(current.getJustifiedEpoch()));
      chainStorage.getJustifiedStorage().set(justifiedRoot);
    }
  }

  private BeaconStateEx pullParentState(BeaconBlock block) {
    Optional<BeaconTuple> parent = tupleStorage.get(block.getParentRoot());
    checkArgument(parent.isPresent(), "No parent for block %s", block);
    BeaconTuple parentTuple = parent.get();

    return parentTuple.getState();
  }

  private boolean exist(BeaconBlock block) {
    Hash32 blockHash = specHelpers.hash_tree_root(block);
    return chainStorage.getBlockStorage().get(blockHash).isPresent();
  }

  private boolean hasParent(BeaconBlock block) {
    return chainStorage.getBlockStorage().get(block.getParentRoot()).isPresent();
  }

  /**
   * There is no sense in importing block with a slot that is too far in the future.
   *
   * @param block block to run check on.
   * @return true if block should be rejected, false otherwise.
   */
  private boolean rejectedByTime(BeaconBlock block) {
    SlotNumber nextToCurrentSlot =
        specHelpers.get_current_slot(recentlyProcessed.getState(), schedulers.getCurrentTime()).increment();

    return block.getSlot().greater(nextToCurrentSlot);
  }

  private BeaconStateEx applyEmptySlotTransitionsTillBlock(BeaconStateEx source, BeaconBlock block) {
    BeaconStateEx result = source;
    for (SlotNumber slot : result.getSlot().increment().iterateTo(block.getSlot())) {
      result = perSlotTransition.apply(result);
      if (specHelpers.is_epoch_end(result.getSlot())) {
        result = perEpochTransition.apply(result);
      }
    }
    // the slot at block should be processed before the block
    result = perSlotTransition.apply(result);

    return result;
  }

  @Override
  public Publisher<BeaconTuple> getBlockStatesStream() {
    return blockStream;
  }
}

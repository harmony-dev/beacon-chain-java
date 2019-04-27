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
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class DefaultBeaconChain implements MutableBeaconChain {
  private static final Logger logger = LogManager.getLogger(DefaultBeaconChain.class);

  private final BeaconChainSpec spec;
  private final BlockTransition<BeaconStateEx> initialTransition;
  private final EmptySlotTransition preBlockTransition;
  private final BlockTransition<BeaconStateEx> blockTransition;
  private final BeaconBlockVerifier blockVerifier;
  private final BeaconStateVerifier stateVerifier;

  private final BeaconChainStorage chainStorage;
  private final BeaconTupleStorage tupleStorage;

  private final SimpleProcessor<BeaconTupleDetails> blockStream;
  private final Schedulers schedulers;

  private BeaconTuple recentlyProcessed;

  public DefaultBeaconChain(
      BeaconChainSpec spec,
      BlockTransition<BeaconStateEx> initialTransition,
      EmptySlotTransition preBlockTransition,
      BlockTransition<BeaconStateEx> blockTransition,
      BeaconBlockVerifier blockVerifier,
      BeaconStateVerifier stateVerifier,
      BeaconChainStorage chainStorage,
      Schedulers schedulers) {
    this.spec = spec;
    this.initialTransition = initialTransition;
    this.preBlockTransition = preBlockTransition;
    this.blockTransition = blockTransition;
    this.blockVerifier = blockVerifier;
    this.stateVerifier = stateVerifier;
    this.chainStorage = chainStorage;
    this.tupleStorage = chainStorage.getTupleStorage();
    this.schedulers = schedulers;

    blockStream = new SimpleProcessor<>(schedulers.reactorEvents(), "DefaultBeaconChain.block");
  }

  @Override
  public void init() {
    if (tupleStorage.isEmpty()) {
      initializeStorage();
    }
    this.recentlyProcessed = fetchRecentTuple();
    blockStream.onNext(new BeaconTupleDetails(recentlyProcessed));
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
    BeaconBlock initialGenesis = spec.get_empty_block();
    BeaconStateEx initialState =
        initialTransition.apply(BeaconStateEx.getEmpty(spec.getConstants()), initialGenesis);

    Hash32 initialStateRoot = spec.hash_tree_root(initialState);
    BeaconBlock genesis = initialGenesis.withStateRoot(initialStateRoot);
    Hash32 genesisRoot = spec.signed_root(genesis);
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

    long s = System.nanoTime();

    BeaconStateEx parentState = pullParentState(block);

    BeaconStateEx preBlockState = preBlockTransition.apply(parentState, block.getSlot());
    VerificationResult blockVerification =
        blockVerifier.verify(block, preBlockState);
    if (!blockVerification.isPassed()) {
      logger.warn("Block verification failed: " + blockVerification + ": " +
          block.toString(spec.getConstants(), parentState.getGenesisTime(), spec::signed_root));
      return false;
    }

    BeaconStateEx postBlockState = blockTransition.apply(preBlockState, block);

    VerificationResult stateVerification =
        stateVerifier.verify(postBlockState, block);
    if (!stateVerification.isPassed()) {
      logger.warn("State verification failed: " + stateVerification);
      return false;
    }

    BeaconTuple newTuple = BeaconTuple.of(block, postBlockState);
    tupleStorage.put(newTuple);
    updateFinality(parentState, postBlockState);

    chainStorage.commit();

    long total = System.nanoTime() - s;

    this.recentlyProcessed = newTuple;
    blockStream.onNext(new BeaconTupleDetails(block, preBlockState, postBlockState, postBlockState));

    logger.info(
        "new block inserted: {} in {}s",
        newTuple
            .getBlock()
            .toString(
                spec.getConstants(),
                newTuple.getState().getGenesisTime(),
                spec::signed_root),
        String.format("%.3f", ((double) total) / 1_000_000_000d));

    return true;
  }

  @Override
  public BeaconTuple getRecentlyProcessed() {
    return recentlyProcessed;
  }

  private void updateFinality(BeaconState previous, BeaconState current) {
    if (previous.getFinalizedEpoch().less(current.getFinalizedEpoch())) {
      Hash32 finalizedRoot =
          spec.get_block_root(
              current, spec.get_epoch_start_slot(current.getFinalizedEpoch()));
      chainStorage.getFinalizedStorage().set(finalizedRoot);
    }
    if (previous.getCurrentJustifiedEpoch().less(current.getCurrentJustifiedEpoch())) {
      Hash32 justifiedRoot =
          spec.get_block_root(
              current, spec.get_epoch_start_slot(current.getCurrentJustifiedEpoch()));
      chainStorage.getJustifiedStorage().set(justifiedRoot);
    }
  }

  private BeaconStateEx pullParentState(BeaconBlock block) {
    Optional<BeaconTuple> parent = tupleStorage.get(block.getPreviousBlockRoot());
    checkArgument(parent.isPresent(), "No parent for block %s", block);
    BeaconTuple parentTuple = parent.get();

    return parentTuple.getState();
  }

  private boolean exist(BeaconBlock block) {
    Hash32 blockHash = spec.signed_root(block);
    return chainStorage.getBlockStorage().get(blockHash).isPresent();
  }

  private boolean hasParent(BeaconBlock block) {
    return chainStorage.getBlockStorage().get(block.getPreviousBlockRoot()).isPresent();
  }

  /**
   * There is no sense in importing block with a slot that is too far in the future.
   *
   * @param block block to run check on.
   * @return true if block should be rejected, false otherwise.
   */
  private boolean rejectedByTime(BeaconBlock block) {
    SlotNumber nextToCurrentSlot =
        spec.get_current_slot(recentlyProcessed.getState(), schedulers.getCurrentTime()).increment();

    return block.getSlot().greater(nextToCurrentSlot);
  }

  @Override
  public Publisher<BeaconTupleDetails> getBlockStatesStream() {
    return blockStream;
  }
}

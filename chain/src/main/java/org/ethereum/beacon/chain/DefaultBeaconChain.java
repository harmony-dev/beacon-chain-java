package org.ethereum.beacon.chain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconTupleStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

public class DefaultBeaconChain implements MutableBeaconChain {
  private static final Logger logger = LogManager.getLogger(DefaultBeaconChain.class);

  private final BeaconChainSpec spec;
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
      EmptySlotTransition preBlockTransition,
      BlockTransition<BeaconStateEx> blockTransition,
      BeaconBlockVerifier blockVerifier,
      BeaconStateVerifier stateVerifier,
      BeaconChainStorage chainStorage,
      Schedulers schedulers) {
    this.spec = spec;
    this.preBlockTransition = preBlockTransition;
    this.blockTransition = blockTransition;
    this.blockVerifier = blockVerifier;
    this.stateVerifier = stateVerifier;
    this.chainStorage = chainStorage;
    this.tupleStorage = chainStorage.getTupleStorage();
    this.schedulers = schedulers;

    blockStream = new SimpleProcessor<>(schedulers.events(), "DefaultBeaconChain.block");
  }

  @Override
  public void init() {
    if (tupleStorage.isEmpty()) {
      throw new IllegalStateException("Couldn't start from empty storage");
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

  private Hash32 getAncestor(Hash32 root, SlotNumber slot) {
    Optional<BeaconBlock> beaconBlock = chainStorage.getBlockStorage().get(root);
    if (!beaconBlock.isPresent()) {
      throw new IllegalArgumentException("Cannot find block " + root);
    }
    return getAncestor(root, beaconBlock.get(), slot);
  }

  private Hash32 getAncestor(Hash32 root, BeaconBlock block, SlotNumber slot) {
    if (block.getSlot().greater(slot)) {
      return getAncestor(block.getParentRoot(), slot);
    } else if (block.getSlot().equals(slot)) {
      return root;
    } else {
      return Hash32.ZERO;
    }
  }

  @Override
  public synchronized ImportResult insert(BeaconBlock block) {
    if (rejectedByTime(block)) {
      return ImportResult.ExpiredBlock;
    }

    if (exist(block)) {
      return ImportResult.ExistingBlock;
    }

    if (!hasParent(block)) {
      return ImportResult.NoParent;
    }

    long s = System.nanoTime();

    BeaconStateEx parentState = pullParentState(block);

    BeaconStateEx preBlockState = preBlockTransition.apply(parentState, block.getSlot());
    VerificationResult blockVerification =
        blockVerifier.verify(block, preBlockState);
    if (!blockVerification.isPassed()) {
      logger.warn("Block verification failed: " + blockVerification + ": " +
          block.toString(spec.getConstants(), parentState.getGenesisTime(), spec::signing_root));
      return ImportResult.InvalidBlock;
    }

    BeaconStateEx postBlockState = blockTransition.apply(preBlockState, block);

    VerificationResult stateVerification =
        stateVerifier.verify(postBlockState, block);
    if (!stateVerification.isPassed()) {
      logger.warn("State verification failed: " + stateVerification);
      return ImportResult.StateMismatch;
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
                spec::signing_root),
        String.format("%.3f", ((double) total) / 1_000_000_000d));

    return ImportResult.OK;
  }

  @Override
  public BeaconTuple getRecentlyProcessed() {
    return recentlyProcessed;
  }

  private void updateFinality(BeaconState previous, BeaconState current) {
    if (!previous.getFinalizedCheckpoint().equals(current.getFinalizedCheckpoint())) {
      chainStorage.getFinalizedStorage().set(current.getFinalizedCheckpoint());
    }
    Checkpoint storeChkpt = chainStorage.getJustifiedStorage().get().get();
    Checkpoint currentJustifiedCheckpoint = current.getCurrentJustifiedCheckpoint();
    if (storeChkpt.getEpoch().less(currentJustifiedCheckpoint.getEpoch())) {
      chainStorage.getBestJustifiedStorage().set(currentJustifiedCheckpoint);
      if (shouldUpdateJustifiedCheckpoint(currentJustifiedCheckpoint)) {
        chainStorage.getJustifiedStorage().set(currentJustifiedCheckpoint);
      }
    }
  }

  private boolean shouldUpdateJustifiedCheckpoint(Checkpoint new_justified_checkpoint) {
    // if compute_slots_since_epoch_start(get_current_slot(store)) < SAFE_SLOTS_TO_UPDATE_JUSTIFIED:
    //    return True
    SlotNumber currentSlot =
        spec.get_current_slot(recentlyProcessed.getState(), schedulers.getCurrentTime());
    if (spec.compute_slots_since_epoch_start(currentSlot)
        .less(spec.getConstants().getSafeSlotsToUpdateJustified())) {
      return true;
    }

    // new_justified_block = store.blocks[new_justified_checkpoint.root]
    // if new_justified_block.slot <= compute_start_slot_at_epoch(store.justified_checkpoint.epoch):
    //   return False
    BeaconBlock new_justified_block =
        chainStorage.getBlockStorage().get(new_justified_checkpoint.getRoot()).get();
    Checkpoint justifiedChkpt = chainStorage.getJustifiedStorage().get().get();
    if (new_justified_block
        .getSlot()
        .lessEqual(spec.compute_start_slot_at_epoch(justifiedChkpt.getEpoch()))) {
      return false;
    }

    // if not (
    //   get_ancestor(store, new_justified_checkpoint.root,
    // store.blocks[store.justified_checkpoint.root].slot) ==
    //     store.justified_checkpoint.root
    // ):
    //   return False
    if (!getAncestor(
            new_justified_checkpoint.getRoot(),
            chainStorage.getBlockStorage().get(justifiedChkpt.getRoot()).get().getSlot())
        .equals(justifiedChkpt.getRoot())) {
      return false;
    }
    // return True
    return true;
  }

  private BeaconStateEx pullParentState(BeaconBlock block) {
    Optional<BeaconTuple> parent = tupleStorage.get(block.getParentRoot());
    checkArgument(parent.isPresent(), "No parent for block %s", block);
    BeaconTuple parentTuple = parent.get();

    return parentTuple.getState();
  }

  private boolean exist(BeaconBlock block) {
    Hash32 blockHash = spec.signing_root(block);
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
        spec.get_current_slot(recentlyProcessed.getState(), schedulers.getCurrentTime()).increment();

    return block.getSlot().greater(nextToCurrentSlot);
  }

  @Override
  public Publisher<BeaconTupleDetails> getBlockStatesStream() {
    return blockStream;
  }
}

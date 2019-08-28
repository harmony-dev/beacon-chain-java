package org.ethereum.beacon.chain.pool.registry;

import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.StatefulProcessor;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Registers and manages attestation that were made to not yet known block.
 *
 * <p>There are two main use cases:
 *
 * <ul>
 *   <li>Pass attestation on {@link #add(ReceivedAttestation)} method. This method checks whether
 *       attestation block exists or not. If it exists then attestation is not added to the registry
 *       and the call will return {@code false}, otherwise, attestation will be added to the
 *       registry and {@link true} will be returned.
 *   <li>When new imported block comes attestations are checked against it. If there are
 *       attestations made to that block they are evicted from pool and should be forwarded to
 *       upstream processor.
 * </ul>
 *
 * <p>Attestations that haven't been identified for a certain perido of time are purged from the
 * pool. This part of the logic is based on {@link #feedNewSlot(SlotNumber)} calls. Effectively,
 * pool contains attestation which target epoch lays between {@code previous_epoch} and {@code
 * current_epoch + epoch_lookahead}.
 *
 * <p><strong>Note:</strong> this implementation is not thread-safe.
 */
public class UnknownAttestationPool implements AttestationRegistry, StatefulProcessor {

  /** A number of tracked epochs: previous_epoch + current_epoch + epoch_lookahead. */
  private final EpochNumber trackedEpochs;
  /** A queue that maintains pooled attestations. */
  private final Queue queue;
  /** A block storage. */
  private final BeaconBlockStorage blockStorage;
  /** A beacon chain spec. */
  private final BeaconChainSpec spec;
  /** A lower time frame boundary of attestation queue. */
  private EpochNumber currentBaseLine;

  public UnknownAttestationPool(
      BeaconBlockStorage blockStorage, BeaconChainSpec spec, EpochNumber lookahead, long size) {
    this.blockStorage = blockStorage;
    this.spec = spec;
    this.trackedEpochs = EpochNumber.of(2).plus(lookahead);
    this.queue = new Queue(trackedEpochs, size);
  }

  @Override
  public boolean add(ReceivedAttestation attestation) {
    assert isInitialized();

    AttestationData data = attestation.getMessage().getData();

    // beacon block has not yet been imported
    // it implies that source and target blocks might have not been imported too
    if (blockStorage.get(data.getBeaconBlockRoot()).isPresent()) {
      return false;
    } else {
      queue.add(data.getTarget().getEpoch(), data.getBeaconBlockRoot(), attestation);
      return true;
    }
  }

  /**
   * Processes recently imported block.
   *
   * @param block a block.
   * @return a list of attestations that have been identified by the block.
   */
  public List<ReceivedAttestation> feedNewImportedBlock(BeaconBlock block) {
    EpochNumber blockEpoch = spec.compute_epoch_of_slot(block.getSlot());
    // blockEpoch < currentBaseLine || blockEpoch >= currentBaseLine + TRACKED_EPOCHS
    if (blockEpoch.less(currentBaseLine)
        || blockEpoch.greaterEqual(currentBaseLine.plus(trackedEpochs))) {
      return Collections.emptyList();
    }

    Hash32 blockRoot = spec.hash_tree_root(block);
    return queue.evict(blockRoot);
  }

  /**
   * Processes new slot.
   *
   * @param slotNumber a slot number.
   */
  public void feedNewSlot(SlotNumber slotNumber) {
    EpochNumber currentEpoch = spec.compute_epoch_of_slot(slotNumber);
    EpochNumber baseLine =
        currentEpoch.equals(spec.getConstants().getGenesisEpoch())
            ? currentEpoch
            : currentEpoch.decrement();
    queue.moveBaseLine(baseLine);

    this.currentBaseLine = baseLine;
  }

  @Override
  public boolean isInitialized() {
    return currentBaseLine != null;
  }
}

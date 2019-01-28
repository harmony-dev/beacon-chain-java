package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;
import java.util.List;
import java.util.Optional;

public interface BeaconBlockStorage extends HashKeyStorage<Hash32, BeaconBlock> {
  /**
   * @return maxStoredSlot or -1 if storage is empty
   */
  long getMaxSlot();

  default boolean isEmpty() {
    return getMaxSlot() == -1;
  }

  List<Hash32> getSlotBlocks(long slot);

  /**
   * Searches for all children with limit slot distance from parent
   *
   * @param parent Start block hash
   * @param limit Slot limit for forward children search
   * @return list of children
   */
  List<BeaconBlock> getChildren(Hash32 parent, int limit);

  /**
   * Returns justified block, which the closest one to the slot (and justified block is before the
   * slot). So this is justified block for this slot.
   *
   * <p>Justified block is the descendant of finalized block with the highest slot number that has
   * been justified for at least EPOCH_LENGTH slots. (A block B is justified if there is a
   * descendant of B in store the processing of which sets B as justified.) If no such descendant
   * exists justified block is equal to finalized block.
   *
   * @param limit Limits number of slots to search
   */
  Optional<BeaconBlock> getJustifiedBlock(long slot, int limit);

  /**
   * Returns finalized block, which the closest one to the slot (and finalized block is before the
   * slot). So this is finalized block for this slot.
   *
   * <p>Finalized block with the highest slot number before the input slot. (A block B is finalized
   * if there is a descendant of B in store the processing of which sets B as finalized.)
   *
   * @param limit Limits number of slots to search
   */
  Optional<BeaconBlock> getFinalizedBlock(long slot, int limit);

  boolean justify(Hash32 blockHash);

  boolean deJustify(Hash32 blockHash);

  boolean finalize(Hash32 blockHash);
}

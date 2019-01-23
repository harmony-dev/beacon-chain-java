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

  Optional<Hash32> getSlotJustifiedBlock(long slot);

  Optional<Hash32> getSlotFinalizedBlock(long slot);

  void addJustifiedHash(Hash32 justifiedHash);

  void addFinalizedHash(Hash32 finalizedHash);
}

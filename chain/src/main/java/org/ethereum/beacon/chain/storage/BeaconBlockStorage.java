package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;
import java.util.List;

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
}

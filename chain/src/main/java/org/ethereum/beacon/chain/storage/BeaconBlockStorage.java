package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;
import java.util.List;
import java.util.Optional;

public interface BeaconBlockStorage extends HashKeyStorage<Hash32, BeaconBlock> {

  /**
   * @return Current canonical head
   * @throws IllegalStateException if storage is empty
   */
  Hash32 getCanonicalHead();

  void reorgTo(Hash32 newCanonicalBlock);

  /**
   * @return maxStoredSlot or -1 if storage is empty
   */
  long getMaxSlot();

  default boolean isEmpty() {
    return getMaxSlot() == -1;
  }

  List<Hash32> getSlotBlocks(long slot);

  Optional<Hash32> getSlotCanonicalBlock(long slot);

  /**
   * Searches for all children with limit slot distance from parent
   *
   * @param parent Start block hash
   * @param limit Slot limit for forward children search
   * @return list of children
   */
  List<BeaconBlock> getChildren(Hash32 parent, int limit);
}

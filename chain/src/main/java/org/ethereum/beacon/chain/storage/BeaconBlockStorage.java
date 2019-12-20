package org.ethereum.beacon.chain.storage;

import java.util.List;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public interface BeaconBlockStorage extends HashKeyStorage<Hash32, SignedBeaconBlock> {
  /**
   * @return maxStoredSlot or {@link UInt64#MAX_VALUE} if storage is empty
   */
  SlotNumber getMaxSlot();

  default boolean isEmpty() {
    return getMaxSlot().equals(UInt64.MAX_VALUE);
  }

  List<Hash32> getSlotBlocks(SlotNumber slot);

  /**
   * Searches for all children with limit slot distance from parent
   *
   * @param parent Start block hash
   * @param limit Slot limit for forward children search
   * @return list of children
   */
  List<SignedBeaconBlock> getChildren(Hash32 parent, int limit);
}

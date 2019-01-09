package org.ethereum.beacon.chain.storage;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;

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

}

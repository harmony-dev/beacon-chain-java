package org.ethereum.beacon.chain.storage;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconBlockStorage extends HashKeyStorage<Hash32, BeaconBlock> {

  Optional<Hash32> getCanonicalHead();

  void reorgTo(Hash32 newCanonicalBlock);

  long getMaxSlot();

  List<Hash32> getSlotBlocks(long slot);

  Optional<Hash32> getSlotCanonicalBlock(long slot);

}

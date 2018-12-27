package org.ethereum.beacon.chain.storage;

import java.util.Optional;
import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconBlockStorage extends HashKeyStorage<Hash32, BeaconBlock> {

  Optional<BeaconBlock> getCanonicalHead();

  void reorgTo(BeaconBlock block);
}

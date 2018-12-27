package org.ethereum.beacon.chain.storage;

import java.util.Optional;
import org.ethereum.beacon.core.BeaconBlock;

public interface BeaconBlockStorage extends Hash32KeyStorage<BeaconBlock> {

  Optional<BeaconBlock> getCanonicalHead();
}

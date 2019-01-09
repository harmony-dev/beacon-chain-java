package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.core.BeaconState;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconStateStorage extends HashKeyStorage<Hash32, BeaconState> {

}

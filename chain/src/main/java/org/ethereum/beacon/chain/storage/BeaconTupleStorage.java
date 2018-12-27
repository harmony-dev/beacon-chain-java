package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.core.BeaconState;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconTupleStorage extends HashKeyStorage<Hash32, BeaconTuple> {

}

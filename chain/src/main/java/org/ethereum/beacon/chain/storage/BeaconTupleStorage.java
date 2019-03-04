package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.chain.BeaconTuple;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconTupleStorage extends HashKeyStorage<Hash32, BeaconTuple> {
  boolean isEmpty();
}

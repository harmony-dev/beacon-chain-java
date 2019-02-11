package org.ethereum.beacon.chain.storage;

import tech.pegasys.artemis.ethereum.core.Hash32;

public interface BeaconTupleStorage extends HashKeyStorage<Hash32, BeaconTuple> {
  boolean isEmpty();
}

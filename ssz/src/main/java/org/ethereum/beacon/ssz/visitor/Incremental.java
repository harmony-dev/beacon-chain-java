package org.ethereum.beacon.ssz.visitor;

interface Incremental {
  SSZIncrementalHasher.SSZIncrementalTracker getTracker();
}

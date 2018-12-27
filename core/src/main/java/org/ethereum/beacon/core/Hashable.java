package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash32;

public interface Hashable {

  Hash32 getHash();
}

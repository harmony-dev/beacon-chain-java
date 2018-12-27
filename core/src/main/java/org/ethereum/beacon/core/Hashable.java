package org.ethereum.beacon.core;

import tech.pegasys.artemis.ethereum.core.Hash;

public interface Hashable<H extends Hash> {

  H getHash();
}

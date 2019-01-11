package org.ethereum.beacon.core;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class BeaconState implements Hashable {
  public static final BeaconState EMPTY = new BeaconState();

  @Override
  public Hash32 getHash() {
    return Hash32.ZERO;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BeaconState that = (BeaconState) o;
    return that.getHash().equals(((BeaconState) o).getHash());
  }
}

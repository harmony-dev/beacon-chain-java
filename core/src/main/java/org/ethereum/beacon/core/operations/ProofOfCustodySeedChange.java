package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.util.ssz.annotation.SSZSerializable;

@SSZSerializable
public class ProofOfCustodySeedChange {

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProofOfCustodySeedChange that = (ProofOfCustodySeedChange) o;
    return true;
  }
}

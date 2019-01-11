package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class ProofOfCustodyChallenge {

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ProofOfCustodyChallenge that = (ProofOfCustodyChallenge) o;
    return true;
  }
}

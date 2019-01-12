package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.ssz.annotation.SSZSerializable;

/** Stub for Phase 1. */
@SSZSerializable
public class CustodyResponse {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return true;
  }
}

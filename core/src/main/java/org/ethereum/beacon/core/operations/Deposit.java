package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.util.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.Arrays;
import java.util.Objects;

@SSZSerializable
public class Deposit {
  private final Hash32[] merkleBranch;
  private final UInt64 merkleTreeIndex;
  private final DepositData depositData;

  public Deposit(Hash32[] merkleBranch, UInt64 merkleTreeIndex, DepositData depositData) {
    this.merkleBranch = merkleBranch;
    this.merkleTreeIndex = merkleTreeIndex;
    this.depositData = depositData;
  }

  public Hash32[] getMerkleBranch() {
    return merkleBranch;
  }

  public UInt64 getMerkleTreeIndex() {
    return merkleTreeIndex;
  }

  public DepositData getDepositData() {
    return depositData;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Deposit deposit = (Deposit) o;
    return Arrays.equals(merkleBranch, deposit.merkleBranch) &&
        merkleTreeIndex.equals(deposit.merkleTreeIndex) &&
        depositData.equals(deposit.depositData);
  }
}

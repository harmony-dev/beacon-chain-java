package org.ethereum.beacon.core.operations;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

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
}

package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.Arrays;

/**
 * Requests to add validator to the validator registry.
 *
 * @see BeaconBlockBody
 * @see DepositData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#deposit>Deposit
 *     in the spec</a>
 */
@SSZSerializable
public class Deposit {

  /** A branch of receipt's Merkle trie of the deposit contract on PoW net. */
  @SSZ
  private final Hash32[] merkleBranch;
  /** An index of receipt's entry in the trie. */
  @SSZ
  private final UInt64 merkleTreeIndex;
  /** Deposit data. */
  @SSZ
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
        Objects.equal(merkleTreeIndex, deposit.merkleTreeIndex) &&
        Objects.equal(depositData, deposit.depositData);
  }
}

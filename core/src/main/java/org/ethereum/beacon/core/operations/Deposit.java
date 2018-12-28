package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Requests to add validator to the validator set.
 *
 * @see BeaconBlockBody
 * @see DepositData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#deposit>Deposit
 *     in the spec</a>
 */
public class Deposit {

  /** A branch of receipt's Merkle trie of the deposit contract on PoW net. */
  private final Hash32[] merkleBranch;
  /** An index of receipt's entry in the trie. */
  private final UInt64 merkleTreeIndex;
  /** Deposit data. */
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

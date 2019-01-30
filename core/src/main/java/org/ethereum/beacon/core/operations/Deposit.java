package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import java.util.List;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

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
  @SSZ private final List<Hash32> merkleBranch;
  /** An index of receipt's entry in the trie. */
  @SSZ private final UInt64 depositIndex;
  /** Deposit data. */
  @SSZ private final DepositData depositData;

  public Deposit(List<Hash32> merkleBranch, UInt64 depositIndex, DepositData depositData) {
    this.merkleBranch = merkleBranch;
    this.depositIndex = depositIndex;
    this.depositData = depositData;
  }

  public List<Hash32> getMerkleBranch() {
    return merkleBranch;
  }

  public UInt64 getDepositIndex() {
    return depositIndex;
  }

  public DepositData getDepositData() {
    return depositData;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Deposit deposit = (Deposit) o;
    return merkleBranch.equals(deposit.merkleBranch)
        && Objects.equal(depositIndex, deposit.depositIndex)
        && Objects.equal(depositData, deposit.depositData);
  }
}

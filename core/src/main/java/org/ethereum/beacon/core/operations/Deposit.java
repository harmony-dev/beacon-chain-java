package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Requests to add validator to the validator registry.
 *
 * @see BeaconBlockBody
 * @see DepositData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.0/specs/core/0_beacon-chain.md#deposit>Deposit</a>
 *     in the spec.
 */
@SSZSerializable
public class Deposit {

  /** A branch of receipt's Merkle trie of the deposit contract on PoW net. */
  @SSZ(vectorLengthVar = "spec.DEPOSIT_CONTRACT_TREE_DEPTH")
  private final ReadVector<Integer, Hash32> proof;
  /** Deposit data. */
  @SSZ private final DepositData data;

  public Deposit(ReadVector<Integer, Hash32> proof, UInt64 index, DepositData data) {
    this.proof = proof;
    this.data = data;
  }

  public Deposit(ReadVector<Integer, Hash32> proof, DepositData data) {
    this.proof = proof;
    this.data = data;
  }

  public ReadVector<Integer, Hash32> getProof() {
    return proof;
  }

  public UInt64 getIndex() {
    return UInt64.ZERO;
  }

  public DepositData getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Deposit deposit = (Deposit) o;
    return proof.equals(deposit.proof) && Objects.equal(data, deposit.data);
  }

  @Override
  public int hashCode() {
    int result = proof.hashCode();
    result = 31 * result + data.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Deposit["
        + "pubkey="
        + data.getPubKey()
        + "withdrawalCredentials="
        + data.getWithdrawalCredentials()
        + "amount="
        + data.getAmount()
        + "]";
  }
}

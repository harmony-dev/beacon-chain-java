package org.ethereum.beacon.core.operations.deposit;

import org.ethereum.beacon.core.operations.Deposit;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;

/**
 * An input parameters of deposit contract.
 *
 * @see DepositData
 * @see Deposit
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#depositinput">DepositInput
 *     in the spec</a>
 */
public class DepositInput {

  /** BLS public key. */
  private final Bytes48 pubKey;
  /** Withdrawal credentials. */
  private final Hash32 withdrawalCredentials;
  /** Initial RANDAO commitment. */
  private final Hash32 randaoCommitment;
  /** Initial proof of custody commitment. */
  private final Hash32 custodyCommitment;
  /** A BLS signature of this {@link DepositInput} */
  private final Bytes96 proofOfPossession;

  public DepositInput(
      Bytes48 pubKey,
      Hash32 withdrawalCredentials,
      Hash32 randaoCommitment,
      Hash32 custodyCommitment,
      Bytes96 proofOfPossession) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.randaoCommitment = randaoCommitment;
    this.custodyCommitment = custodyCommitment;
    this.proofOfPossession = proofOfPossession;
  }

  public Bytes48 getPubKey() {
    return pubKey;
  }

  public Hash32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  public Hash32 getRandaoCommitment() {
    return randaoCommitment;
  }

  public Hash32 getCustodyCommitment() {
    return custodyCommitment;
  }

  public Bytes96 getProofOfPossession() {
    return proofOfPossession;
  }
}

package org.ethereum.beacon.core.operations.deposit;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
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
@SSZSerializable
public class DepositInput {

  /** BLS public key. */
  @SSZ private final BLSPubkey pubKey;
  /** Withdrawal credentials. */
  @SSZ private final Hash32 withdrawalCredentials;
  /** A BLS signature of this {@link DepositInput} */
  @SSZ private final BLSSignature proofOfPossession;

  public DepositInput(
      BLSPubkey pubKey,
      Hash32 withdrawalCredentials,
      BLSSignature proofOfPossession) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.proofOfPossession = proofOfPossession;
  }

  public BLSPubkey getPubKey() {
    return pubKey;
  }

  public Hash32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  public BLSSignature getProofOfPossession() {
    return proofOfPossession;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DepositInput that = (DepositInput) o;
    return Objects.equal(pubKey, that.pubKey)
        && Objects.equal(withdrawalCredentials, that.withdrawalCredentials)
        && Objects.equal(proofOfPossession, that.proofOfPossession);
  }
}

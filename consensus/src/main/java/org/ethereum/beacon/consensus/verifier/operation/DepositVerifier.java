package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Verifies {@link Deposit} beacon chain operation.
 *
 * @see Deposit
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#deposits-1">Deposits</a>
 *     in the spec.
 */
public class DepositVerifier implements OperationVerifier<Deposit> {

  private ChainSpec chainSpec;
  private SpecHelpers specHelpers;

  public DepositVerifier(ChainSpec chainSpec, SpecHelpers specHelpers) {
    this.chainSpec = chainSpec;
    this.specHelpers = specHelpers;
  }

  @Override
  public VerificationResult verify(Deposit deposit, BeaconState state) {
    BytesValue serializedDepositData =
        specHelpers.serialized_deposit_data(deposit.getDepositData());
    Hash32 serializedDataHash = specHelpers.hash(serializedDepositData);

    if (!specHelpers.verify_merkle_branch(
        serializedDataHash,
        deposit.getMerkleBranch(),
        chainSpec.getDepositContractTreeDepth(),
        deposit.getDepositIndex(),
        state.getLatestDepositRoot())) {

      return failedResult(
          "merkle proof verification failed, serialized_data_hash = %s", serializedDataHash);
    }

    return PASSED;
  }
}

package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Verifies {@link Deposit} beacon chain operation.
 *
 * @see Deposit
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#deposits-1">Deposits</a>
 *     in the spec.
 */
public class DepositVerifier implements OperationVerifier<Deposit> {

  private final BeaconChainSpec spec;
  private final SSZSerializer ssz = new SSZBuilder().buildSerializer();

  public DepositVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(Deposit deposit, BeaconState state) {
    Hash32 serializedDataHash = spec.hash_tree_root(deposit.getData());

    if (!spec.verify_merkle_branch(
        serializedDataHash,
        deposit.getProof(),
        spec.getConstants().getDepositContractTreeDepth(),
        deposit.getIndex(),
        state.getLatestEth1Data().getDepositRoot())) {

      return failedResult(
          "merkle proof verification failed, serialized_data_hash = %s", serializedDataHash);
    }

    return PASSED;
  }
}

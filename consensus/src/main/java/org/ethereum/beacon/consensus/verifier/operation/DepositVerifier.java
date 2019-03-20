package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.ssz.Serializer;
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

  private final SpecHelpers spec;
  private final Serializer ssz = Serializer.annotationSerializer();

  public DepositVerifier(SpecHelpers spec) {
    this.spec = spec;
  }

  BytesValue serialize(DepositData depositData) {
    // Let serialized_deposit_data be the serialized form of deposit.deposit_data.
    // It should be 8 bytes for deposit_data.amount
    // followed by 8 bytes for deposit_data.timestamp
    // and then the DepositInput bytes.
    // That is, it should match deposit_data in the Ethereum 1.0 deposit contract of which
    // the hash was placed into the Merkle tree.
    return depositData
        .getAmount().toBytesBigEndian()
        .concat(depositData.getTimestamp().toBytesBigEndian())
        .concat(ssz.encode2(depositData.getDepositInput()));
  }

  @Override
  public VerificationResult verify(Deposit deposit, BeaconState state) {
    BytesValue serializedDepositData = serialize(deposit.getDepositData());
    Hash32 serializedDataHash = spec.hash(serializedDepositData);

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

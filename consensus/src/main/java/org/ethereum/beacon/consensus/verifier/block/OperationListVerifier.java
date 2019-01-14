package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;

import java.util.List;
import java.util.function.Function;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

/**
 * An abstract class for a family of beacon chain operation verifiers.
 *
 * @param <T> beacon chain operation type.
 * @see OperationVerifier
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#operations">Operations</a>
 *     in the spec.
 */
public abstract class OperationListVerifier<T> implements BeaconBlockVerifier {

  private OperationVerifier<T> operationVerifier;
  private Function<BeaconBlock, List<T>> operationListExtractor;
  private int maxOperationsInList;

  protected OperationListVerifier(
      OperationVerifier<T> operationVerifier,
      Function<BeaconBlock, List<T>> operationListExtractor,
      int maxOperationsInList) {
    this.operationVerifier = operationVerifier;
    this.operationListExtractor = operationListExtractor;
    this.maxOperationsInList = maxOperationsInList;
  }

  @Override
  public VerificationResult verify(BeaconBlock block, BeaconState state) {
    List<T> operations = operationListExtractor.apply(block);

    if (operations.size() > maxOperationsInList) {
      return VerificationResult.createdFailed(
          "%s max number exceeded, should be at most %d but got %d",
          getType().getSimpleName(), maxOperationsInList, operations.size());
    }

    for (T operation : operations) {
      VerificationResult result = operationVerifier.verify(operation, state);
      if (result != PASSED) {
        return result;
      }
    }

    return PASSED;
  }

  protected abstract Class<T> getType();
}

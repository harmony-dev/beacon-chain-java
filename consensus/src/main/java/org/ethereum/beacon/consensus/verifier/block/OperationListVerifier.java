package org.ethereum.beacon.consensus.verifier.block;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.createdFailed;

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
 * <p>Accepts an instance of {@link OperationVerifier}, operation list extractor and max number of
 * operations that is allowed to be included in the list.
 *
 * <p>Its {@link #verify(BeaconBlock, BeaconState)} method does following things:
 *
 * <ul>
 *   <li>Extracts a list of operations with {@link #operationListExtractor}.
 *   <li>Verifies that the list has at most {@link #maxOperationsInList} items.
 *   <li>Verifies each operation in the list by applying {@link #operationVerifier} to it.
 * </ul>
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

    for (int i = 0; i < operations.size(); i++) {
      VerificationResult result = operationVerifier.verify(operations.get(i), state);
      if (result != PASSED) {
        return createdFailed(
            "%s with index %d: %s", getType().getSimpleName(), i, result.getMessage());
      }
    }

    return PASSED;
  }

  protected abstract Class<T> getType();
}

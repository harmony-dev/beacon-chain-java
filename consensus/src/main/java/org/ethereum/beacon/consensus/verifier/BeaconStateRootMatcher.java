package org.ethereum.beacon.consensus.verifier;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;

/**
 * Matches hash of the state to {@link BeaconBlock#stateRoot} of given block.
 *
 * <p>Fails if given state doesn't match to the state of the block.
 */
public class BeaconStateRootMatcher implements BeaconStateVerifier {

  private final SpecHelpers specHelpers;

  public BeaconStateRootMatcher(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
  }

  @Override
  public VerificationResult verify(BeaconState state, BeaconBlock block) {
    if (block.getStateRoot().equals(specHelpers.hash_tree_root(state))) {
      return VerificationResult.PASSED;
    } else {
      return VerificationResult.failedResult(
          "State root doesn't match, expected %s but got %s",
          block.getStateRoot(), specHelpers.hash_tree_root(state));
    }
  }
}

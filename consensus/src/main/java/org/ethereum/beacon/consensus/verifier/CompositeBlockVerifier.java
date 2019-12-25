package org.ethereum.beacon.consensus.verifier;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;

/**
 * Aggregates a number of block verifiers.
 *
 * <p>Verifiers are triggered according to the order that they were added with. If one of
 * verification has failed it returns immediately with this failed result.
 *
 * @see BeaconBlockVerifier
 */
public class CompositeBlockVerifier implements BeaconBlockVerifier {

  private List<BeaconBlockVerifier> verifiers;

  public CompositeBlockVerifier(List<BeaconBlockVerifier> verifiers) {
    this.verifiers = verifiers;
  }

  @Override
  public VerificationResult verify(SignedBeaconBlock block, BeaconState state) {
    for (BeaconBlockVerifier verifier : verifiers) {
      VerificationResult result = verifier.verify(block, state);
      if (result != PASSED) {
        return result;
      }
    }
    return PASSED;
  }

  public static class Builder {
    private List<BeaconBlockVerifier> verifiers;

    private Builder() {
      this.verifiers = new ArrayList<>();
    }

    public static Builder createNew() {
      return new Builder();
    }

    public Builder with(BeaconBlockVerifier verifier) {
      this.verifiers.add(verifier);
      return this;
    }

    public CompositeBlockVerifier build() {
      assert verifiers.size() > 0;
      return new CompositeBlockVerifier(verifiers);
    }
  }
}

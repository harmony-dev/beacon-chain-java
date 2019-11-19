package org.ethereum.beacon.validator;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.ValidatorIndex;

/**
 * An interface of beacon chain attester. A part of beacon validator logic.
 *
 * <p>Attester creates attestations with empty signature. Use {@link BeaconAttestationSigner} in
 * order to get signed attestation.
 *
 * @see ValidatorService
 * @see BeaconAttestationSigner
 */
public interface BeaconChainAttester {

  /**
   * Given state creates an attestation to a slot of the state with specified head block.
   *
   * @param validatorIndex index of the validator.
   * @param index committee index.
   * @param state a state that attestation is based on.
   * @param head a head of the chain.
   * @return an attestation with empty signature.
   */
  Attestation attest(
      ValidatorIndex validatorIndex, CommitteeIndex index, BeaconState state, BeaconBlock head);
}

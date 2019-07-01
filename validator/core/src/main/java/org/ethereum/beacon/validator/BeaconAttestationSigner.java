package org.ethereum.beacon.validator;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.validator.attester.BeaconAttestationSignerImpl;
import org.ethereum.beacon.validator.crypto.MessageSigner;

/**
 * Updates given attestation with its signature.
 *
 * @see BeaconChainAttester
 */
public interface BeaconAttestationSigner {

  /**
   * Given attestation creates signature and returns signed attestation.
   *
   * @param attestation an attestation with empty signature.
   * @param fork fork object from the state.
   * @return signed attestation.
   */
  Attestation sign(Attestation attestation, Fork fork);

  static BeaconAttestationSigner getInstance(
      BeaconChainSpec spec, MessageSigner<BLSSignature> signer) {
    return new BeaconAttestationSignerImpl(spec, signer);
  }
}

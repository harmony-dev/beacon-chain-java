package org.ethereum.beacon.validator;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.validator.proposer.RandaoGeneratorImpl;

/**
 * Given epoch number generates RANDAO reveal.
 *
 * @see BeaconChainProposer
 */
public interface RandaoGenerator {

  /**
   * Creates RANDAO signature.
   *
   * @param epoch an epoch.
   * @param fork fork object from the state.
   * @return generated RANDAO siganture.
   */
  BLSSignature reveal(EpochNumber epoch, Fork fork);

  static RandaoGenerator getInstance(BeaconChainSpec spec, MessageSigner<BLSSignature> signer) {
    return new RandaoGeneratorImpl(spec, signer);
  }
}

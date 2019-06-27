package org.ethereum.beacon.validator;

import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.validator.crypto.MessageSigner;

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
   * @param signer BLS signer used to sign off on a message.
   * @return generated RANDAO siganture.
   */
  BLSSignature reveal(EpochNumber epoch, Fork fork, MessageSigner<BLSSignature> signer);
}

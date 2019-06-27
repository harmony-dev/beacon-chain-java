package org.ethereum.beacon.validator;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.validator.crypto.MessageSigner;

/**
 * Given block fills RANDAO reveal and updates block's signature.
 *
 * @see BeaconChainProposer
 */
public interface BeaconBlockSigner {

  /**
   * Given block reveals randao, signs off on this block and returns a block with randao and
   * signature.
   *
   * @param block a block with empty signature.
   * @param fork fork object from the state.
   * @param signer BLS signer used to sign off on a message.
   * @return a block with revealed randao and signature.
   */
  BeaconBlock sign(BeaconBlock block, Fork fork, MessageSigner<BLSSignature> signer);
}

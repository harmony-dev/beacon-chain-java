package org.ethereum.beacon.validator;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.validator.proposer.BeaconBlockSignerImpl;

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
   * @param state Beacon state
   * @return a block with revealed randao and signature.
   */
  SignedBeaconBlock sign(BeaconBlock block, BeaconState state);

  static BeaconBlockSigner getInstance(BeaconChainSpec spec, MessageSigner<BLSSignature> signer) {
    return new BeaconBlockSignerImpl(spec, signer);
  }
}

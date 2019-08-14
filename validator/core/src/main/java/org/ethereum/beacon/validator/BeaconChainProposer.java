package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.BLSSignature;

/**
 * An interface of beacon chain proposer. A part of beacon validator logic.
 *
 * <p>Proposer creates {@link BeaconBlock} with empty signature. Use {@link BeaconBlockSigner} in
 * order to sign off on a block.
 *
 * <p>Use {@link RandaoGenerator} to generate RANDAO reveal.
 *
 * @see ValidatorService
 */
public interface BeaconChainProposer {

  /**
   * Create beacon chain block with empty signature and randao reveal.
   *
   * @param observableState a state on top of which new block is created.
   * @param randaoReveal revealed randao signature.
   * @return created block.
   */
  BeaconBlock propose(ObservableBeaconState observableState, BLSSignature randaoReveal);
}

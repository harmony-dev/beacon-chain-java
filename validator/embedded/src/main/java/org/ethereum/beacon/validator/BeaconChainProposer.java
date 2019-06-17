package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.util.bytes.Bytes32;

/**
 * An interface of beacon chain proposer. A part of beacon validator logic.
 *
 * @see ValidatorService
 */
public interface BeaconChainProposer {

  /**
   * Create beacon chain block.
   *
   * <p>Created block should be ready to be imported in the chain and propagated to the network.
   *
   * @param observableState a state on top of which new block is created.
   * @param signer an instance that signs off on a block.
   * @return created block.
   */
  BeaconBlock propose(ObservableBeaconState observableState, MessageSigner<BLSSignature> signer);

  /**
   * Prepares builder with complete block without signature to sign it off later.
   * Part of {@link #propose(ObservableBeaconState, MessageSigner)} logic without signing and distribution
   * @param slot                Slot number we are going to create block for
   * @param randaoReveal        Signer RANDAO reveal
   * @param observableState     A state on top of which new block is created.
   * @return builder with unsigned block, ready for sign and distribution
   */
  BeaconBlock.Builder prepareBuilder(
      SlotNumber slot, BLSSignature randaoReveal, ObservableBeaconState observableState);

  /**
   * Given a state returns graffiti value.
   *
   * @param state a state.
   * @return graffiti value.
   */
  default Bytes32 getGraffiti(BeaconState state) {
    return Bytes32.ZERO;
  }
}

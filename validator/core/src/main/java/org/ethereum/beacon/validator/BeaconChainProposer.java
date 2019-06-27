package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.BLSSignature;
import tech.pegasys.artemis.util.bytes.Bytes32;

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
   * @param state a state on top of which new block is created.
   * @param randaoReveal revealed randao signature.
   * @param pendingOperations beacon operations yet not included on chain.
   * @return created block.
   */
  BeaconBlock propose(
      BeaconState state, BLSSignature randaoReveal, PendingOperations pendingOperations);

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

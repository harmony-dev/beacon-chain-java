package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.util.bytes.Bytes96;

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
   * @param signer an instance that signs off on {@link ProposalSignedData}.
   * @return created block.
   */
  BeaconBlock propose(ObservableBeaconState observableState, MessageSigner<Bytes96> signer);
}

package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.uint.UInt64;

import static org.ethereum.beacon.core.spec.SignatureDomains.BEACON_PROPOSER;
import static org.ethereum.beacon.core.spec.SignatureDomains.RANDAO;

/**
 * An interface of beacon chain proposer. A part of beacon validator logic.
 *
 * @see ValidatorService
 */
public interface BeaconChainProposer {

  /**
   * Signs off on a block.
   *
   * @param state state at the slot of created block.
   * @param block block
   * @param signer message signer.
   * @param spec Beacon chain spec
   * @return signature of proposal signed data.
   */
  static BLSSignature getProposalSignature(
      BeaconState state,
      BeaconBlock block,
      MessageSigner<BLSSignature> signer,
      BeaconChainSpec spec) {
    Hash32 proposalRoot = spec.signing_root(block);
    UInt64 domain = spec.get_domain(state, BEACON_PROPOSER);
    return signer.sign(proposalRoot, domain);
  }

  /**
   * Returns next RANDAO reveal.
   *
   * @param state state at the slot of created block.
   * @param signer message signer.
   * @param spec Beacon chain spec
   * @return next RANDAO reveal.
   */
  static BLSSignature getRandaoReveal(
      BeaconState state, MessageSigner<BLSSignature> signer, BeaconChainSpec spec) {
    Hash32 hash = spec.hash_tree_root(spec.slot_to_epoch(state.getSlot()));
    UInt64 domain = spec.get_domain(state, RANDAO);
    return signer.sign(hash, domain);
  }

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
   * Prepares builder with complete block without signature to sign it off later. Part of {@link
   * #propose(ObservableBeaconState, MessageSigner)} logic without signing and distribution
   *
   * @param slot Slot number we are going to create block for
   * @param randaoReveal Signer RANDAO reveal
   * @param observableState A state on top of which new block is created.
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

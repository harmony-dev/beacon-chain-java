package org.ethereum.beacon.validator.proposer;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlock.Builder;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.BeaconProposerSpec;
import org.ethereum.beacon.validator.ValidatorService;
import org.ethereum.beacon.validator.crypto.MessageSigner;

/**
 * An implementation of beacon chain proposer.
 *
 * @see BeaconChainProposer
 * @see ValidatorService
 */
public class BeaconChainProposerImpl implements BeaconChainProposer {

  /** Proposer routines * */
  private final BeaconProposerSpec beaconProposerSpec;
  /** The spec. */
  private BeaconChainSpec spec;
  /** Per-block state transition. */
  private BlockTransition<BeaconStateEx> perBlockTransition;
  /** Eth1 deposit contract. */
  private DepositContract depositContract;

  public BeaconChainProposerImpl(
      BeaconChainSpec spec,
      BlockTransition<BeaconStateEx> perBlockTransition,
      DepositContract depositContract) {
    this.spec = spec;
    this.beaconProposerSpec = new BeaconProposerSpec(spec);
    this.perBlockTransition = perBlockTransition;
    this.depositContract = depositContract;
  }

  @Override
  public BeaconBlock propose(
      ObservableBeaconState observableState, MessageSigner<BLSSignature> signer) {
    BeaconStateEx state = observableState.getLatestSlotState();
    BLSSignature randaoReveal = getRandaoReveal(state, signer);
    Builder builder =
        beaconProposerSpec.prepareBuilder(
            perBlockTransition, depositContract, state.getSlot(), randaoReveal, observableState);
    BLSSignature signature = getProposalSignature(state, builder.build(), signer);
    builder.withSignature(signature);

    return builder.build();
  }

  /**
   * Signs off on a block.
   *
   * @param state state at the slot of created block.
   * @param block block
   * @param signer message signer.
   * @return signature of proposal signed data.
   */
  private BLSSignature getProposalSignature(
      BeaconState state, BeaconBlock block, MessageSigner<BLSSignature> signer) {
    return beaconProposerSpec.getProposalSignature(state, block, signer);
  }

  /**
   * Returns next RANDAO reveal.
   *
   * @param state state at the slot of created block.
   * @param signer message signer.
   * @return next RANDAO reveal.
   */
  private BLSSignature getRandaoReveal(BeaconState state, MessageSigner<BLSSignature> signer) {
    return beaconProposerSpec.getRandaoReveal(state, signer);
  }
}

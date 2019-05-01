package org.ethereum.beacon.consensus.transition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.MutableBeaconState;

/**
 * Per-slot transition, which happens at every slot.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#per-slot-processing">Per-slot
 *     processing</a> in the spec.
 */
public class PerSlotTransition implements StateTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(PerSlotTransition.class);

  private final BeaconChainSpec spec;

  public PerSlotTransition(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx stateEx) {
    logger.trace(() -> "Applying slot transition to state: (" +
        spec.hash_tree_root(stateEx).toStringShort() + ") " +
        stateEx.toString(spec.getConstants(), spec::signing_root));
    TransitionType.SLOT.checkCanBeAppliedAfter(stateEx.getTransition());

    MutableBeaconState state = stateEx.createMutableCopy();

    spec.advance_slot(state);

    BeaconStateEx ret = new BeaconStateExImpl(state.createImmutable(), TransitionType.SLOT);

    logger.trace(() -> "Slot transition result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " +
        ret.toString(spec.getConstants(), spec::signing_root));

    return ret;
  }
}

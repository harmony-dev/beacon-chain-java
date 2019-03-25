package org.ethereum.beacon.consensus.transition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.MutableBeaconState;

/**
 * State caching, which happens at the start of every slot.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#state-caching">State
 *     caching</a> in the spec.
 */
public class StateCachingTransition implements StateTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(StateCachingTransition.class);

  private final SpecHelpers spec;

  public StateCachingTransition(SpecHelpers spec) {
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx source) {
    logger.debug(() -> "Applying state caching to state: (" +
        spec.hash_tree_root(source).toStringShort() + ") " + source.toString(spec.getConstants()));

    TransitionType.CACHING.checkCanBeAppliedAfter(source.getTransition());

    MutableBeaconState state = source.createMutableCopy();

    spec.cache_state(state);

    BeaconStateEx ret =
        new BeaconStateExImpl(
            state.createImmutable(), source.getHeadBlockHash(), TransitionType.CACHING);

    logger.debug(() -> "State caching result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " + ret.toString(spec.getConstants()));

    return ret;
  }
}

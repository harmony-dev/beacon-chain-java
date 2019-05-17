package org.ethereum.beacon.consensus.transition;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.core.MutableBeaconState;

/**
 * State caching, which happens at the start of every slot.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.1/specs/core/0_beacon-chain.md#state-caching">State
 *     caching</a> in the spec.
 */
public class StateCachingTransition implements StateTransition<BeaconStateEx> {
  private static final Logger logger = LogManager.getLogger(StateCachingTransition.class);

  private final BeaconChainSpec spec;

  public StateCachingTransition(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public BeaconStateEx apply(BeaconStateEx source) {
    logger.debug(() -> "Applying state caching to state: (" +
        spec.hash_tree_root(source).toStringShort() + ") " +
        source.toString(spec.getConstants(), spec::signing_root));

    TransitionType.CACHING.checkCanBeAppliedAfter(source.getTransition());

    MutableBeaconState state = source.createMutableCopy();

    spec.cache_state(state);

    BeaconStateEx ret = new BeaconStateExImpl(state.createImmutable(), TransitionType.CACHING);

    logger.debug(() -> "State caching result state: (" +
        spec.hash_tree_root(ret).toStringShort() + ") " +
        ret.toString(spec.getConstants(), spec::signing_root));

    return ret;
  }
}

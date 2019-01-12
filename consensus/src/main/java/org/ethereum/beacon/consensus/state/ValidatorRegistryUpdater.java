package org.ethereum.beacon.consensus.state;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import tech.pegasys.artemis.util.uint.UInt24;

/**
 * Interface to modify validator registry.
 *
 * <p>Extends {@link ValidatorRegistryReader}, hence, all reader methods are also available.
 *
 * <p>Can be instantly created from {@link BeaconState} by {@link #fromState(BeaconState,
 * ChainSpec)} method. When modification is finished there is an {@link #applyTo(BeaconState)}
 * method to produce a new state by applying the changes to the previous one.
 */
public interface ValidatorRegistryUpdater extends ValidatorRegistryReader {

  /**
   * Creates an updater instance by fetching required data from {@link BeaconState}.
   *
   * @param state beacon state.
   * @param chainSpec beacon chain spec.
   * @return constructed updater.
   */
  static ValidatorRegistryUpdater fromState(BeaconState state, ChainSpec chainSpec) {
    return new InMemoryValidatorRegistryUpdater(
        state.extractValidatorRegistry(),
        state.extractValidatorBalances(),
        state.getValidatorRegistryDeltaChainTip(),
        state.getSlot(),
        chainSpec);
  }

  /**
   * Processes validator's deposit.
   *
   * <p>Creates new validator record if given public key is not yet registered. Otherwise, top ups
   * validator's balance.
   *
   * @param deposit a deposit record.
   * @return an index of processed validator.
   */
  UInt24 processDeposit(Deposit deposit);

  /**
   * Activates specified validator.
   *
   * @param index validator index.
   * @throws IndexOutOfBoundsException if index is invalid.
   */
  void activate(UInt24 index);

  /**
   * Produces a new {@link BeaconState} from the given one and updates made to the validator
   * registry.
   *
   * @param origin origin state which changes are applied to.
   * @return a new state object containing all the updates.
   */
  BeaconState applyTo(BeaconState origin);
}

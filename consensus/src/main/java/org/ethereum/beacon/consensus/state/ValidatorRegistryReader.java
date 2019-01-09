package org.ethereum.beacon.consensus.state;

import java.util.Optional;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.pow.DepositContract;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Interface to read from validator registry.
 *
 * <p>Can be instantly created from {@link BeaconState} by {@link #fromState(BeaconState)} method.
 */
public interface ValidatorRegistryReader {

  /**
   * Creates a reader instance by fetching required data from {@link BeaconState}.
   *
   * @param state beacon state.
   * @return constructed reader.
   */
  static ValidatorRegistryReader fromState(BeaconState state) {
    return new InMemoryValidatorRegistryUpdater(
        state.extractValidatorRegistry(),
        state.extractValidatorBalances(),
        state.getValidatorRegistryDeltaChainTip(),
        state.getSlot());
  }

  /**
   * Returns validator's balance capped by {@link DepositContract#MAX_DEPOSIT} value.
   *
   * @param index index of the validator.
   * @return a deposit value in GWei.
   * @throws IndexOutOfBoundsException if index is invalid.
   */
  UInt64 getEffectiveBalance(UInt24 index);

  /**
   * Returns validator record with given index.
   *
   * @param index validator index.
   * @return validator record.
   * @throws IndexOutOfBoundsException if index is invalid.
   */
  ValidatorRecord get(UInt24 index);

  /**
   * Returns a number of validator records.
   *
   * @return a size.
   */
  UInt24 size();

  /**
   * Returns validator record that public key is equal to given one.
   *
   * @param pubKey a public key.
   * @return validator record if it's been found.
   */
  Optional<ValidatorRecord> getByPubKey(Bytes48 pubKey);
}

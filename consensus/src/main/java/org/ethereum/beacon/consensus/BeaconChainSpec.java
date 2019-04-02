package org.ethereum.beacon.consensus;

import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.consensus.spec.BlockProcessing;
import org.ethereum.beacon.consensus.spec.EpochProcessing;
import org.ethereum.beacon.consensus.spec.ForkChoice;
import org.ethereum.beacon.consensus.spec.HelperFunction;
import org.ethereum.beacon.consensus.spec.OnGenesis;
import org.ethereum.beacon.consensus.spec.SlotProcessing;
import org.ethereum.beacon.consensus.spec.StateCaching;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.Millis;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.Hashes;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Beacon chain spec.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md">Beacon
 *     chain</a>
 */
public interface BeaconChainSpec
    extends HelperFunction,
        OnGenesis,
        ForkChoice,
        StateCaching,
        EpochProcessing,
        SlotProcessing,
        BlockProcessing {

  SpecConstants DEFAULT_CONSTANTS = new SpecConstants() {};

  /**
   * Creates a BeaconChainSpec instance with given {@link SpecConstants} and time supplier,
   * {@link Hashes#keccak256(BytesValue)} as a hash function and {@link SSZObjectHasher} as an object
   * hasher.
   *
   * @param constants a chain getConstants().
   *    <code>Schedulers::currentTime</code> is passed
   * @return spec helpers instance.
   */
  static BeaconChainSpec createWithSSZHasher(@Nonnull SpecConstants constants) {
    Objects.requireNonNull(constants);

    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
    ObjectHasher<Hash32> sszHasher = SSZObjectHasher.create(hashFunction);
    return new BeaconChainSpecImpl(constants, hashFunction, sszHasher);
  }

  static BeaconChainSpec createWithDefaults() {
    return createWithSSZHasher(DEFAULT_CONSTANTS);
  }

  default SlotNumber get_current_slot(BeaconState state, long systemTime) {
    Millis currentTime = Millis.of(systemTime);
    assertTrue(state.getGenesisTime().lessEqual(currentTime.getSeconds()));
    Time sinceGenesis = currentTime.getSeconds().minus(state.getGenesisTime());
    return SlotNumber.castFrom(sinceGenesis.dividedBy(getConstants().getSecondsPerSlot()))
        .plus(getConstants().getGenesisSlot());
  }

  default Time get_slot_start_time(BeaconState state, SlotNumber slot) {
    return state
        .getGenesisTime()
        .plus(getConstants().getSecondsPerSlot().times(slot.minus(getConstants().getGenesisSlot())));
  }

  default Time get_slot_middle_time(BeaconState state, SlotNumber slot) {
    return get_slot_start_time(state, slot).plus(getConstants().getSecondsPerSlot().dividedBy(2));
  }

  default boolean is_epoch_end(SlotNumber slot) {
    return slot.increment().modulo(getConstants().getSlotsPerEpoch()).equals(SlotNumber.ZERO);
  }

  default boolean is_current_slot(BeaconState state, long systemTime) {
    return state.getSlot().equals(get_current_slot(state, systemTime));
  }
}
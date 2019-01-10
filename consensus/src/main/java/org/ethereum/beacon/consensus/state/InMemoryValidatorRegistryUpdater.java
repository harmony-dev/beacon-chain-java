package org.ethereum.beacon.consensus.state;

import static com.google.common.base.Preconditions.checkElementIndex;
import static org.ethereum.beacon.core.state.ValidatorRecord.ENTRY_EXIT_DELAY;
import static org.ethereum.beacon.core.state.ValidatorStatusFlag.EMPTY;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconChainSpec.Genesis;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.state.ValidatorRecord.Builder;
import org.ethereum.beacon.core.state.ValidatorStatusFlag;
import org.ethereum.beacon.core.state.ValidatorRegistryDeltaBlock;
import org.ethereum.beacon.core.state.ValidatorRegistryDeltaBlock.FlagCodes;
import org.ethereum.beacon.pow.DepositContract;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * A straightforward implementation of {@link ValidatorRegistryUpdater} which keeps registry
 * modifications in memory.
 */
public class InMemoryValidatorRegistryUpdater implements ValidatorRegistryUpdater {

  /** Validator records. */
  private List<ValidatorRecord> records;
  /** Validator balances. */
  private List<UInt64> balances;
  /**
   * Hash of latest update in validator's update chain. Recalculated each time when any validator
   * record is updated.
   */
  private Hash32 deltaChainTip;
  /** Current slot number in the state. */
  private UInt64 currentSlot;
  /** Beacon chain parameters. */
  private ChainSpec chainSpec;

  InMemoryValidatorRegistryUpdater(
      List<ValidatorRecord> records,
      List<UInt64> balances,
      Hash32 deltaChainTip,
      UInt64 currentSlot,
      ChainSpec chainSpec) {
    assert records.size() == balances.size();

    this.records = records;
    this.balances = balances;
    this.deltaChainTip = deltaChainTip;
    this.currentSlot = currentSlot;
    this.chainSpec = chainSpec;
  }

  @Override
  public UInt24 processDeposit(Deposit deposit) {
    DepositInput input = deposit.getDepositData().getDepositInput();
    Bytes48 pubKey = input.getPubKey();

    // seek for existing entry or creates a new one and increase its balance
    Entry entry = searchByPubKey(pubKey).orElse(createNewEntry(input));
    Entry toppedUpEntry = entry.increaseBalance(deposit.getDepositData().getValue());
    toppedUpEntry.commit();

    return toppedUpEntry.index;
  }

  @Override
  public void activate(UInt24 index) {
    rangeCheck(index);

    Entry entry = getEntry(index);
    Entry activated = entry.activate();
    activated.commit();

    ValidatorRegistryDeltaBlock delta =
        new ValidatorRegistryDeltaBlock(
            deltaChainTip, index, activated.getPubKey(), FlagCodes.ACTIVATION);
    deltaChainTip = delta.getHash();
  }

  @Override
  public UInt64 getEffectiveBalance(UInt24 index) {
    rangeCheck(index);

    Entry entry = getEntry(index);
    return entry.getEffectiveBalance();
  }

  @Override
  public ValidatorRecord get(UInt24 index) {
    rangeCheck(index);

    Entry entry = getEntry(index);
    return entry.tuple.record;
  }

  @Override
  public UInt24 size() {
    return UInt24.valueOf(records.size());
  }

  @Override
  public Optional<ValidatorRecord> getByPubKey(Bytes48 pubKey) {
    return searchByPubKey(pubKey).flatMap(entry -> Optional.of(entry.tuple.record));
  }

  @Override
  public BeaconState applyTo(BeaconState origin) {
    return BeaconState.Builder.fromState(origin)
        .withValidatorRegistry(records)
        .withValidatorBalances(balances)
        .withValidatorRegistryDeltaChainTip(deltaChainTip)
        .build();
  }

  private void rangeCheck(UInt24 index) {
    checkElementIndex(index.getValue(), records.size());
  }

  private Entry getEntry(UInt24 index) {
    return getEntry(index.getValue());
  }

  private Entry getEntry(int index) {
    assert index < records.size();
    return new Entry(Tuple.of(records.get(index), balances.get(index)), UInt24.valueOf(index));
  }

  /**
   * Creates new {@link Entry} that belongs to a validator registry.
   *
   * <p>Allocates new record at the end of the lists.
   *
   * <p><strong>Note:</strong> returns a record with {@code 0} balance.
   *
   * <p><strong>Note:</strong> runs {@link Entry#commit()} at the end of the call to force
   * insertion.
   *
   * @param input deposit input.
   * @return newly created record.
   */
  private Entry createNewEntry(DepositInput input) {
    Builder builder =
        Builder.fromDepositInput(input)
            .withRandaoLayers(UInt64.ZERO)
            .withActivationSlot(BeaconChainSpec.FAR_FUTURE_SLOT)
            .withExitSlot(BeaconChainSpec.FAR_FUTURE_SLOT)
            .withWithdrawalSlot(BeaconChainSpec.FAR_FUTURE_SLOT)
            .withPenalizedSlot(BeaconChainSpec.FAR_FUTURE_SLOT)
            .withExitCount(UInt64.ZERO)
            .withStatusFlag(EMPTY)
            .withLatestCustodyReseedSlot(Genesis.SLOT)
            .withPenultimateCustodyReseedSlot(Genesis.SLOT);

    Tuple tuple = Tuple.of(builder.build(), UInt64.ZERO);
    UInt24 index = size();
    Entry newEntry = new Entry(tuple, index);
    newEntry.commit();

    return newEntry;
  }

  /**
   * Searches validator by its public key.
   *
   * <p>Returns an {@link Entry} that holds validator's index along with its record.
   *
   * @param pubKey validator's public key.
   * @return validator's entry if found, otherwise, nothing.
   */
  private Optional<Entry> searchByPubKey(Bytes48 pubKey) {
    for (int idx = 0; idx < records.size(); ++idx) {
      if (pubKey.equals(records.get(idx).getPubKey())) {
        return Optional.of(getEntry(idx));
      }
    }
    return Optional.empty();
  }

  /**
   * A helper class that contains {@link Tuple} and its index in the registry.
   *
   * <p>It provides methods that modify validator entries in the registry.
   *
   * <p><strong>Note:</strong> call {@link #commit()} method to apply latest validator state to the
   * registry.
   */
  private class Entry {
    private final Tuple tuple;
    private final UInt24 index;

    private Entry(Tuple tuple, UInt24 index) {
      this.tuple = tuple;
      this.index = index;
    }

    private UInt64 getEffectiveBalance() {
      return UInt64.min(tuple.balance, chainSpec.getMaxDeposit().toGWei());
    }

    private ValidatorStatusFlag getStatus() {
      return tuple.record.getStatusFlags();
    }

    private Bytes48 getPubKey() {
      return tuple.record.getPubKey();
    }

    /**
     * Increases validator's balance by given amount.
     *
     * <p><strong>Note:</strong> does not modify the registry. Use {@link #commit()} to apply
     * balance change.
     *
     * @param amount balance increment.
     * @return entry with new balance.
     */
    private Entry increaseBalance(UInt64 amount) {
      UInt64 newBalance = balances.get(index.getValue()).plus(amount);
      return new Entry(tuple.withBalance(newBalance), index);
    }

    /**
     * Activates validator.
     *
     * <p><strong>Note:</strong> does not modify the registry. Use {@link #commit()} to apply status
     * change.
     *
     * @return activated validator entry.
     */
    private Entry activate() {
      UInt64 activationSlot =
          currentSlot.equals(Genesis.SLOT) ? currentSlot : currentSlot.plus(ENTRY_EXIT_DELAY);

      ValidatorRecord activated =
          ValidatorRecord.Builder.fromRecord(tuple.record)
              .withActivationSlot(activationSlot)
              .build();
      records.set(index.getValue(), activated);

      return new Entry(tuple.withRecord(activated), index);
    }

    /**
     * Flushes entry state to registry lists {@link #records} and {@link #balances} by updating
     * already existing index or appending a new one at the end of the lists.
     */
    private void commit() {
      assert index.getValue() <= records.size();

      if (index.getValue() < records.size()) {
        records.set(index.getValue(), tuple.record);
        balances.set(index.getValue(), tuple.balance);
      } else {
        records.add(tuple.record);
        balances.add(tuple.balance);
      }
    }
  }

  /** Helper class that holds {@link ValidatorRecord} and its balance. */
  private static class Tuple {
    private final ValidatorRecord record;
    private final UInt64 balance;

    private Tuple(ValidatorRecord record, UInt64 balance) {
      this.record = record;
      this.balance = balance;
    }

    static Tuple of(ValidatorRecord record, UInt64 balance) {
      return new Tuple(record, balance);
    }

    private Tuple withRecord(ValidatorRecord record) {
      return new Tuple(record, balance);
    }

    private Tuple withBalance(UInt64 balance) {
      return new Tuple(record, balance);
    }
  }
}

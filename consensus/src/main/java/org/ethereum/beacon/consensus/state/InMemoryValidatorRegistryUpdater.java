package org.ethereum.beacon.consensus.state;

import static com.google.common.base.Preconditions.checkElementIndex;
import static org.ethereum.beacon.core.state.ValidatorRecord.ZERO_BALANCE_VALIDATOR_TTL;
import static org.ethereum.beacon.core.state.ValidatorStatus.PENDING_ACTIVATION;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.state.ValidatorRecord.Builder;
import org.ethereum.beacon.core.state.ValidatorStatus;
import org.ethereum.beacon.core.state.ValidatorRegistryDeltaBlock;
import org.ethereum.beacon.core.state.ValidatorRegistryDeltaBlock.FlagCodes;
import org.ethereum.beacon.pow.DepositContract;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

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

  InMemoryValidatorRegistryUpdater(
      List<ValidatorRecord> records,
      List<UInt64> balances,
      Hash32 deltaChainTip,
      UInt64 currentSlot) {
    assert records.size() == balances.size();

    this.records = records;
    this.balances = balances;
    this.deltaChainTip = deltaChainTip;
    this.currentSlot = currentSlot;
  }

  @Override
  public UInt24 processDeposit(Deposit deposit) {
    DepositInput input = deposit.getDepositData().getDepositInput();
    Bytes48 pubKey = input.getPubKey();

    // seek for existing entry or creates a new one and increase its balance
    Entry entry = searchByPubKey(pubKey).orElse(createNewEntry(input));
    Entry toppedUpEntry = entry.increaseBalance(deposit.getDepositData().getValue());

    return toppedUpEntry.index;
  }

  @Override
  public void activate(UInt24 index) {
    rangeCheck(index);

    Entry entry = getEntry(index);
    if (entry.getStatus() != PENDING_ACTIVATION) {
      return;
    }

    Entry activated = entry.activate();

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
   * Creates new {@link Entry} inserts it into validator record and balance lists.
   *
   * <p><strong>Note:</strong> returns a record with {@code 0} balance.
   *
   * @param input deposit input.
   * @return newly created record.
   */
  private Entry createNewEntry(DepositInput input) {
    Builder builder =
        Builder.fromDepositInput(input)
            .withStatus(PENDING_ACTIVATION)
            .withLatestStatusChangeSlot(currentSlot)
            .withRandaoLayers(UInt64.ZERO)
            .withExitCount(UInt64.ZERO)
            .withLatestCustodyReseedSlot(UInt64.ZERO)
            .withPenultimateCustodyReseedSlot(UInt64.ZERO);

    Tuple tuple = Tuple.of(builder.build(), UInt64.ZERO);

    Optional<UInt24> emptyIndex = findMinEmptyValidatorIndex();
    if (emptyIndex.isPresent()) {
      insert(emptyIndex.get(), tuple);
      return new Entry(tuple, emptyIndex.get());
    } else {
      UInt24 index = append(tuple);
      return new Entry(tuple, index);
    }
  }

  /**
   * Returns minimum index which corresponds to a record considered as empty.
   *
   * @return an index if it exists, otherwise, nothing.
   */
  private Optional<UInt24> findMinEmptyValidatorIndex() {
    for (int idx = 0; idx < records.size(); ++idx) {
      Entry entry = getEntry(idx);
      if (entry.isEmpty()) {
        return Optional.of(entry.index);
      }
    }
    return Optional.empty();
  }

  /**
   * Adds validator record and its balance to the end of the lists.
   *
   * @param tuple a tuple holding record and balance.
   * @return index of appended validator record.
   */
  private UInt24 append(Tuple tuple) {
    UInt24 index = size();
    records.add(tuple.record);
    balances.add(tuple.balance);
    return index;
  }

  /**
   * Inserts validator record and its balance into specified position in the lists.
   *
   * @param index index of the position.
   * @param tuple validator record and balance.
   */
  private void insert(UInt24 index, Tuple tuple) {
    assert index.getValue() < records.size();
    records.set(index.getValue(), tuple.record);
    balances.set(index.getValue(), tuple.balance);
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

  /** A helper class that contains {@link Tuple} and its index in the registry. */
  private class Entry {
    private final Tuple tuple;
    private final UInt24 index;

    private Entry(Tuple tuple, UInt24 index) {
      this.tuple = tuple;
      this.index = index;
    }

    private UInt64 getEffectiveBalance() {
      return UInt64.min(tuple.balance, DepositContract.MAX_DEPOSIT.toGWei());
    }

    /**
     * Increases validator's balance by given amount.
     *
     * @param amount balance increment.
     * @return new balance.
     */
    private Entry increaseBalance(UInt64 amount) {
      UInt64 newBalance = balances.get(index.getValue()).plus(amount);
      balances.set(index.getValue(), newBalance);
      return new Entry(tuple.withBalance(newBalance), index);
    }

    private ValidatorStatus getStatus() {
      return tuple.record.getStatus();
    }

    private Bytes48 getPubKey() {
      return tuple.record.getPubKey();
    }

    private Entry activate() {
      ValidatorRecord activated =
          ValidatorRecord.Builder.fromRecord(tuple.record)
              .withStatus(ValidatorStatus.ACTIVE)
              .withLatestStatusChangeSlot(currentSlot)
              .build();
      records.set(index.getValue(), activated);

      return new Entry(tuple.withRecord(activated), index);
    }

    private boolean isEmpty() {
      if (!tuple.balance.equals(UInt64.ZERO)) {
        return false;
      }
      UInt64 ttlSlot = tuple.record.getLatestStatusChangeSlot().plus(ZERO_BALANCE_VALIDATOR_TTL);
      if (ttlSlot.compareTo(currentSlot) > 0) {
        return false;
      }

      return true;
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

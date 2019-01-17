package org.ethereum.beacon.consensus;

import java.util.Collections;
import java.util.Comparator;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.spec.ValidatorRegistryDeltaFlags;
import org.ethereum.beacon.core.spec.ValidatorStatusFlags;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.state.ValidatorRegistryDeltaBlock;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.crypto.MessageParameters;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.*;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

/**
 * https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#helper-functions
 */
public class SpecHelpers {
  private final ChainSpec spec;

  public SpecHelpers(ChainSpec spec) {
    this.spec = spec;
  }

  public Hash32 hash(BytesValue data) {
    return Hashes.keccak256(data);
  }

  /*
    def get_committee_count_per_slot(active_validator_count: int) -> int:
    return max(
          1,
          min(
              SHARD_COUNT // EPOCH_LENGTH,
    active_validator_count // EPOCH_LENGTH// TARGET_COMMITTEE_SIZE,
          )
      )
   */
  int get_committee_count_per_slot(int active_validator_count) {
    return max(1,
        min(
            spec.getShardCount()
                .dividedBy(spec.getEpochLength()).getIntValue(),
            UInt64.valueOf(active_validator_count)
                .dividedBy(spec.getEpochLength())
                .dividedBy(spec.getTargetCommitteeSize().getValue())
                .getIntValue()
        ));
  }

  /*
      def get_previous_epoch_committee_count_per_slot(state: BeaconState) -> int:
        previous_active_validators = get_active_validator_indices(state.validator_registry, state.previous_epoch_calculation_slot)
        return get_committee_count_per_slot(len(previous_active_validators))
   */
  public int get_previous_epoch_committee_count_per_slot(BeaconState state) {
    List<UInt24> previous_active_validators = get_active_validator_indices(
        state.getValidatorRegistry().toArray(new ValidatorRecord[0]),
        state.getPreviousEpochCalculationSlot());
    return get_committee_count_per_slot(previous_active_validators.size());
  }

  /*
    def get_current_epoch_committee_count_per_slot(state: BeaconState) -> int:
        current_active_validators = get_active_validator_indices(validators, state.current_epoch_calculation_slot)
        return get_committee_count_per_slot(len(current_active_validators))
   */
  public int get_current_epoch_committee_count_per_slot(BeaconState state) {
    List<UInt24> previous_active_validators = get_active_validator_indices(
        state.getValidatorRegistry().toArray(new ValidatorRecord[0]),
        state.getCurrentEpochCalculationSlot());
    return get_committee_count_per_slot(previous_active_validators.size());
  }

  /*
    Returns the list of ``(committee, shard)`` tuples for the ``slot``.
   */
  public List<Pair<UInt24[], UInt64>> get_shard_committees_at_slot(BeaconState state, UInt64 slot) {
    UInt64 state_epoch_slot = state.getSlot().minus(state.getSlot().modulo(spec.getEpochLength()));
    assertTrue(state_epoch_slot.compareTo(slot.plus(spec.getEpochLength())) <= 0);
    assertTrue(slot.compareTo(state_epoch_slot.plus(spec.getEpochLength())) < 0);

    //    offset = slot % EPOCH_LENGTH
    UInt64 offset = slot.modulo(spec.getEpochLength());

    //    if slot < state_epoch_slot:
    int committees_per_slot;
    List<List<UInt24>> shuffling;
    UInt64 slot_start_shard;
    if (slot.compareTo(state_epoch_slot) < 0) {
      //      committees_per_slot = get_previous_epoch_committees_per_slot(state)
      committees_per_slot = get_previous_epoch_committee_count_per_slot(state);
      //      shuffling = get_shuffling(state.previous_epoch_randao_mix,
      //          state.validator_registry,
      //          state.previous_epoch_calculation_slot)
      shuffling = get_shuffling(state.getPreviousEpochRandaoMix(),
          state.getValidatorRegistry().toArray(new ValidatorRecord[0]),
          state.getPreviousEpochCalculationSlot());
          //      slot_start_shard = (state.previous_epoch_start_shard + committees_per_slot * offset) % SHARD_COUNT
      slot_start_shard = state.getPreviousEpochStartShard()
          .plus(committees_per_slot)
          .times(offset)
          .modulo(spec.getShardCount());
    //    else:
    } else {
      //      committees_per_slot = get_current_epoch_committees_per_slot(state)
      committees_per_slot = get_current_epoch_committee_count_per_slot(state);
      //      shuffling = get_shuffling(state.current_epoch_randao_mix,
      //          state.validator_registry,
      //          state.current_epoch_calculation_slot)
      shuffling = get_shuffling(state.getCurrentEpochRandaoMix(),
          state.getValidatorRegistry().toArray(new ValidatorRecord[0]),
          state.getCurrentEpochCalculationSlot());
      //      slot_start_shard = (state.current_epoch_start_shard + committees_per_slot * offset) % SHARD_COUNT
      slot_start_shard = state.getCurrentEpochStartShard()
          .plus(committees_per_slot)
          .times(offset)
          .modulo(spec.getShardCount());
    }

    //    return [
    //    (shuffling[committees_per_slot * offset + i], (slot_start_shard + i) % SHARD_COUNT)
    //    for i in range(committees_per_slot)
    //    ]
    List<Pair<UInt24[], UInt64>> ret = new ArrayList<>();
    for (int i = 0; i < committees_per_slot; i++) {
      List<UInt24> shuffling1 = shuffling.get(offset.times(committees_per_slot).plus(i).getIntValue());
      UInt24[] shuffling2 = new UInt24[shuffling1.size()];
      for (int i1 = 0; i1 < shuffling1.size(); i1++) {
        shuffling2[i1] = shuffling1.get(i1);
      }
      ret.add(Pair.with(shuffling2, slot_start_shard.plus(i).modulo(spec.getShardCount())));
    }
    return ret;
}

  /*
   first_committee = get_shard_committees_at_slot(state, slot)[0].committee
   return first_committee[slot % len(first_committee)]
  */
  public UInt24 get_beacon_proposer_index(BeaconState state, UInt64 slot) {
    UInt24[] first_committee = get_shard_committees_at_slot(state, slot).get(0).getValue0();
    return first_committee[safeInt(slot.modulo(first_committee.length))];
  }

  /*
   def is_active_validator(validator: ValidatorRecord, slot: int) -> bool:
   """
   Checks if ``validator`` is active.
   """
   return validator.activation_slot <= slot < validator.exit_slot
  */
  public boolean is_active_validator(ValidatorRecord validator, UInt64 slot) {
    return validator.getActivationSlot().compareTo(slot) <= 0
        && slot.compareTo(validator.getExitSlot()) < 0;
  }

  /*
   def get_active_validator_indices(validators: [ValidatorRecord], slot: int) -> List[int]:
   """
   Gets indices of active validators from ``validators``.
   """
   return [i for i, v in enumerate(validators) if is_active_validator(v, slot)]
  */
  public List<UInt24>  get_active_validator_indices(ValidatorRecord[] validators, UInt64 slot) {
    ArrayList<UInt24> ret = new ArrayList<>();
    for (int i = 0; i < validators.length; i++) {
      if (is_active_validator(validators[i], slot)) {
        ret.add(UInt24.valueOf(i));
      }
    }
    return ret;
  }

  /*
      def get_randao_mix(state: BeaconState, slot: int) -> Hash32:
          """
          Returns the randao mix at a recent ``slot``.
          """
          assert state.slot < slot + LATEST_RANDAO_MIXES_LENGTH
          assert slot <= state.slot
          return state.latest_randao_mixes[slot % LATEST_RANDAO_MIXES_LENGTH]
    */
  public Hash32 get_randao_mix(BeaconState state, UInt64 slot) {
    assertTrue(state.getSlot().compareTo(slot.plus(spec.getLatestRandaoMixesLength())) < 0);
    assertTrue(slot.compareTo(state.getSlot()) <= 0);
    return state.getLatestRandaoMixes().get(
        slot.modulo(spec.getLatestRandaoMixesLength()).getIntValue());
  }

  /*
   def shuffle(values: List[Any], seed: Hash32) -> List[Any]:
   """
   Returns the shuffled ``values`` with ``seed`` as entropy.
   """
  */
  public <T> List<T> shuffle(List<T> values, Hash32 seed) {

    //    values_count = len(values)
    int values_count = values.size();

    //    # Entropy is consumed from the seed in 3-byte (24 bit) chunks.
    //        rand_bytes = 3
    //    # The highest possible result of the RNG.
    //        rand_max = 2 ** (rand_bytes * 8) - 1
    int rand_bytes = 3;
    int rand_max = 1 << (rand_bytes * 8 - 1);

    //    # The range of the RNG places an upper-bound on the size of the list that
    //    # may be shuffled. It is a logic error to supply an oversized list.
    //    assert values_count < rand_max
    assertTrue(values_count < rand_max);

    //    output = [x for x in values]
    //    source = seed
    //    index = 0
    List<T> output = new ArrayList<>(values);
    Hash32 source = seed;
    int index = 0;

    //    while index < values_count - 1:
    while (index < values_count - 1) {
      //    # Re-hash the `source` to obtain a new pattern of bytes.
      //    source = hash(source)
      source = hash(source);

      //    # Iterate through the `source` bytes in 3-byte chunks.
      //    for position in range(0, 32 - (32 % rand_bytes), rand_bytes):
      for (int position = 0; position < 32 - (32 % rand_bytes); position += rand_bytes) {
        //    # Determine the number of indices remaining in `values` and exit
        //    # once the last index is reached.
        //    remaining = values_count - index
        //    if remaining == 1:
        //        break
        int remaining = values_count - index;
        if (remaining == 1) {
          break;
        }

        //    # Read 3-bytes of `source` as a 24-bit big-endian integer.
        //    sample_from_source = int.from_bytes(source[position:position + rand_bytes], 'big')
        int sample_from_source = Bytes3.wrap(source, position).asUInt24BigEndian().getValue();

        //    # Sample values greater than or equal to `sample_max` will cause
        //    # modulo bias when mapped into the `remaining` range.
        //    sample_max = rand_max - rand_max % remaining
        int sample_max = rand_max - rand_max % remaining;

        //    # Perform a swap if the consumed entropy will not cause modulo bias.
        //    if sample_from_source < sample_max:
        if (sample_from_source < sample_max) {
          //    # Select a replacement index for the current index.
          //    replacement_position = (sample_from_source % remaining) + index
          int replacement_position = (sample_from_source % remaining) + index;
          //    # Swap the current index with the replacement index.
          //    output[index], output[replacement_position] = output[replacement_position], output[index]
          //    index += 1
          Collections.swap(output, index, replacement_position);
          index += 1;
        }
        //    else:
        //        # The sample causes modulo bias. A new sample should be read.
        //        pass
      }
    }

    return output;
  }

  /*
   def split(values: List[Any], split_count: int) -> List[Any]:
   """
   Splits ``values`` into ``split_count`` pieces.
   """
   list_length = len(values)
   return [
       values[(list_length * i // split_count): (list_length * (i + 1) // split_count)]
       for i in range(split_count)
   ]
  */
  public <T> List<List<T>> split(List<T> values, int split_count) {
    List<List<T>> ret = new ArrayList<>();
    for (int i = 0; i < split_count; i++) {
      int fromIdx = values.size() * i / split_count;
      int toIdx = min(values.size() * (i + 1) / split_count, values.size());
      ret.add(values.subList(fromIdx, toIdx));
    }
    return ret;
  }

  //  def get_shuffling(randao_mix: Hash32,
  //              validators: List[ValidatorRecord],
  //              slot: int) -> List[List[int]]
  //  """
  //  Shuffles ``validators`` into shard committees seeded by ``seed`` and ``slot``.
  //  Returns a list of ``EPOCH_LENGTH * committees_per_slot`` committees where each
  //  committee is itself a list of validator indices.
  //  """
  public List<List<UInt24>> get_shuffling(Hash32 _seed,
                               ValidatorRecord[] validators,
                               UInt64 _slot) {


    //
    //      # Normalizes slot to start of epoch boundary
    //  slot -= slot % EPOCH_LENGTH
    UInt64 slot = _slot.minus(_slot.modulo(spec.getEpochLength()));
    //      active_validator_indices = get_active_validator_indices(validators, slot)
    List<UInt24>  active_validator_indices = get_active_validator_indices(validators, slot);
    //      committees_per_slot = get_committee_count_per_slot(len(active_validator_indices))
    int committees_per_slot = get_committee_count_per_slot(active_validator_indices.size());

    //      # Shuffle
    //      seed = xor(seed, bytes32(slot))
    Hash32 seed = Hash32.wrap(Bytes32s.xor(_seed, Bytes32.leftPad(slot.toBytesBigEndian())));

    //  shuffled_active_validator_indices = shuffle(active_validator_indices, seed)
    List<UInt24> shuffled_active_validator_indices = shuffle(active_validator_indices, seed);
    //    # Split the shuffled list into epoch_length * committees_per_slot pieces
    //    return split(shuffled_active_validator_indices, committees_per_slot * EPOCH_LENGTH)
    return split(shuffled_active_validator_indices,
        spec.getEpochLength()
            .times(committees_per_slot)
            .getIntValue());
  }

  /*
    def merkle_root(values):
    """
    Merkleize ``values`` (where ``len(values)`` is a power of two) and return the Merkle root.
    """
    o = [0] * len(values) + values
    for i in range(len(values) - 1, 0, -1):
        o[i] = hash(o[i * 2] + o[i * 2 + 1])
    return o[1]
   */
  public Hash32 merkle_root(List<? extends BytesValue> values) {
    assertTrue(Integer.bitCount(values.size()) == 1);
    BytesValue[] o = new BytesValue[values.size() * 2];
    for (int i = 0; i < values.size(); i++) {
      o[i + values.size()] = values.get(i);
    }
    for (int i = values.size() - 1; i > 0; i--) {
      o[i] = hash(BytesValue.wrap(o[i * 2], o[i * 2 + 1]));
    }
    return (Hash32) o[1];
  }

  /*
   get_effective_balance(state: State, index: int) -> int:
     """
     Returns the effective balance (also known as "balance at stake") for a ``validator`` with the given ``index``.
     """
     return min(state.validator_balances[index], MAX_DEPOSIT * GWEI_PER_ETH)
  */
  public UInt64 get_effective_balance(BeaconState state, UInt24 validatorIdx) {
    return UInt64s.min(
        state.getValidatorBalances().get(validatorIdx.getValue()),
        spec.getMaxDeposit().toGWei());
  }

  /*
    def integer_squareroot(n: int) -> int:
    """
    The largest integer ``x`` such that ``x**2`` is less than ``n``.
    """
    assert n >= 0
    x = n
    y = (x + 1) // 2
    while y < x:
        x = y
        y = (x + n // x) // 2
    return x
   */
  public UInt64 integer_squareroot(UInt64 n) {
    UInt64 x = n;
    UInt64 y = x.increment().dividedBy(2);
    while (y.compareTo(x) < 0) {
      x = y;
      y = x.plus(n.dividedBy(x)).dividedBy(2);
    }
    return x;
  }

  /*
    def activate_validator(state: BeaconState, index: int, genesis: bool) -> None:
      validator = state.validator_registry[index]

      validator.activation_slot = GENESIS_SLOT if genesis else (state.slot + ENTRY_EXIT_DELAY)
      state.validator_registry_delta_chain_tip = hash_tree_root(
          ValidatorRegistryDeltaBlock(
              latest_registry_delta_root=state.validator_registry_delta_chain_tip,
              validator_index=index,
              pubkey=validator.pubkey,
              slot=validator.activation_slot,
              flag=ACTIVATION,
          )
      )
   */
  public void activate_validator(MutableBeaconState state, UInt24 index, boolean genesis) {
    state.withValidatorRecord(index.getValue(), vb ->
            vb.withActivationSlot(genesis ?
                spec.getGenesisSlot() :
                state.getSlot().plus(spec.getEntryExitDelay())));
    ValidatorRecord validator = state.getValidatorRegistry().get(index.getValue());
    state.withValidatorRegistryDeltaChainTip(hash_tree_root(
        new ValidatorRegistryDeltaBlock(
            state.getValidatorRegistryDeltaChainTip(),
            index,
            validator.getPubKey(),
            validator.getActivationSlot(),
            ValidatorRegistryDeltaFlags.ACTIVATION
        )
    ));
  }

  /*
    def penalize_validator(state: BeaconState, index: int) -> None:
      exit_validator(state, index)
      validator = state.validator_registry[index]
      state.latest_penalized_exit_balances[(state.slot // EPOCH_LENGTH) % LATEST_PENALIZED_EXIT_LENGTH]
          += get_effective_balance(state, index)

      whistleblower_index = get_beacon_proposer_index(state, state.slot)
      whistleblower_reward = get_effective_balance(state, index) // WHISTLEBLOWER_REWARD_QUOTIENT
      state.validator_balances[whistleblower_index] += whistleblower_reward
      state.validator_balances[index] -= whistleblower_reward
      validator.penalized_slot = state.slot
    */
  public void penalize_validator(MutableBeaconState state, int index) {
    exit_validator(state, UInt24.valueOf(index));
    int exitBalanceIdx =
        state
            .getSlot()
            .dividedBy(spec.getEpochLength())
            .modulo(spec.getLatestPenalizedExitLength())
            .getIntValue();
    state.withLatestPenalizedExitBalance(
        exitBalanceIdx,
        balance -> balance.plus(get_effective_balance(state, UInt24.valueOf(index))));

    UInt24 whistleblower_index = get_beacon_proposer_index(state, state.getSlot());
    UInt64 whistleblower_reward =
        get_effective_balance(state, UInt24.valueOf(index))
            .dividedBy(spec.getWhistleblowerRewardQuotient());
    state.withValidatorBalance(
        whistleblower_index, oldVal -> oldVal.plus(whistleblower_reward));
    state.withValidatorBalance(UInt24.valueOf(index), oldVal -> oldVal.minus(whistleblower_reward));
    state.withValidatorRecord(index, vb -> vb.withPenalizedSlot(state.getSlot()));
  }

  /*
   def initiate_validator_exit(state: BeaconState, index: int) -> None:
     validator = state.validator_registry[index]
     validator.status_flags |= INITIATED_EXIT
  */
  public void initiate_validator_exit(MutableBeaconState state, int index) {
    state.withValidatorRecord(
        index, vb -> vb.withStatusFlags(flags -> flags.or(ValidatorStatusFlags.INITIATED_EXIT)));
  }

  /*
    def exit_validator(state: BeaconState, index: int) -> None:
      validator = state.validator_registry[index]

      # The following updates only occur if not previous exited
      if validator.exit_slot <= state.slot + ENTRY_EXIT_DELAY:
          return

      validator.exit_slot = state.slot + ENTRY_EXIT_DELAY

      state.validator_registry_exit_count += 1
      validator.exit_count = state.validator_registry_exit_count
      state.validator_registry_delta_chain_tip = hash_tree_root(
          ValidatorRegistryDeltaBlock(
              latest_registry_delta_root=state.validator_registry_delta_chain_tip,
              validator_index=index,
              pubkey=validator.pubkey,
              slot=validator.exit_slot,
              flag=EXIT,
          )
      )
   */
  public void exit_validator(MutableBeaconState state, UInt24 index) {
    ValidatorRecord validator = state.getValidatorRegistry().get(index.getValue());
    if (validator.getExitSlot()
        .compareTo(state.getSlot().plus(spec.getEntryExitDelay())) <= 0) {
      return;
    }

    state.withValidatorRegistryExitCount(state.getValidatorRegistryExitCount().increment());
    state.withValidatorRecord(index.getValue(), vb -> {
      vb.withExitSlot(state.getSlot().plus(spec.getEntryExitDelay()));
      vb.withExitCount(state.getValidatorRegistryExitCount());
    });
    ValidatorRecord validatorNew = state.getValidatorRegistry().get(index.getValue());

    state.withValidatorRegistryDeltaChainTip(hash_tree_root(
        new ValidatorRegistryDeltaBlock(
            state.getValidatorRegistryDeltaChainTip(),
            index,
            validatorNew.getPubKey(),
            validatorNew.getExitSlot(),
            ValidatorRegistryDeltaFlags.EXIT
        )
    ));
  }

  /*
    def update_validator_registry(state: BeaconState) -> None:
      """
      Update validator registry.
      Note that this function mutates ``state``.
      """
   */
  public void update_validator_registry(MutableBeaconState state) {
    //    # The active validators
    //    active_validator_indices =
    //          get_active_validator_indices(state.validator_registry, state.slot)
    List<UInt24> active_validator_indices = get_active_validator_indices(
        state.getValidatorRegistry().toArray(new ValidatorRecord[0]), state.getSlot());

    //      # The total effective balance of active validators
    //      total_balance =
    //          sum([get_effective_balance(state, i) for i in active_validator_indices])
    UInt64 total_balance = UInt64.ZERO;
    for (UInt24 i : active_validator_indices) {
      total_balance = total_balance.plus(get_effective_balance(state, i));
    }

    //    # The maximum balance churn in Gwei (for deposits and exits separately)
    //    max_balance_churn = max(
    //        MAX_DEPOSIT_AMOUNT,
    //        total_balance // (2 * MAX_BALANCE_CHURN_QUOTIENT)
    //    )
    UInt64 max_balance_churn = UInt64s.max(spec.getMaxDeposit().toGWei(),
        total_balance.dividedBy(spec.getMaxBalanceChurnQuotient().times(2)));

    //    # Activate validators within the allowable balance churn
    //    balance_churn = 0
    //    for index, validator in enumerate(state.validator_registry):
    UInt64 balance_churn = UInt64.ZERO;
    for (int index = 0; index < state.getValidatorRegistry().size(); index++) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      //    if validator.activation_slot > state.slot + ENTRY_EXIT_DELAY
      //       and state.validator_balances[index] >= MAX_DEPOSIT_AMOUNT:
      if (validator.getActivationSlot()
            .compareTo(state.getSlot().plus(spec.getEntryExitDelay())) > 0
          && state.getValidatorBalances().get(index)
            .compareTo(spec.getMaxDeposit().toGWei()) >= 0) {

        //    # Check the balance churn would be within the allowance
        //    balance_churn += get_effective_balance(state, index)
        balance_churn = balance_churn.plus(get_effective_balance(state, UInt24.valueOf(index)));

        //    if balance_churn > max_balance_churn:
        //      break
        if (balance_churn.compareTo(max_balance_churn) > 0) {
          break;
        }

        //    # Activate validator
        //    activate_validator(state, index, False)
        activate_validator(state, UInt24.valueOf(index), false);
      }
    }
    //    # Exit validators within the allowable balance churn
    //     balance_churn = 0
    balance_churn = UInt64.ZERO;
    //    for index, validator in enumerate(state.validator_registry):
    for (int index = 0; index < state.getValidatorRegistry().size(); index++) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      //        if validator.exit_slot > state.slot + ENTRY_EXIT_DELAY
      //                and validator.status_flags & INITIATED_EXIT:
      if (validator.getExitSlot()
          .compareTo(state.getSlot().plus(spec.getEntryExitDelay())) > 0
          && validator.getStatusFlags().or(ValidatorStatusFlags.INITIATED_EXIT) == validator
          .getStatusFlags()) {
        //   # Check the balance churn would be within the allowance
        //   balance_churn += get_effective_balance(state, index)
        balance_churn = balance_churn.plus(get_effective_balance(state, UInt24.valueOf(index)));
        //   if balance_churn > max_balance_churn:
        //       break
        if (balance_churn.compareTo(max_balance_churn) > 0) {
          break;
        }
        //   # Exit validator
        //   exit_validator(state, index)
        exit_validator(state, UInt24.valueOf(index));
      }
    }

    //    state.validator_registry_update_slot = state.slot
    //  FIXME check field name
    state.withValidatorRegistryLatestChangeSlot(state.getSlot());
  }

  /*
    def prepare_validator_for_withdrawal(state: BeaconState, index: int) -> None:
        validator = state.validator_registry[index]
        validator.status_flags |= WITHDRAWABLE
   */
  public void prepare_validator_for_withdrawal(MutableBeaconState state, UInt24 index) {
    state.withValidatorRecord(index.getValue(),
        vb -> vb.withStatusFlags(flags -> flags.or(ValidatorStatusFlags.WITHDRAWABLE)));
  }

  /*
    def process_penalties_and_exits(state: BeaconState) -> None:
   */
  public void process_penalties_and_exits(MutableBeaconState state) {
    //    # The active validators
    //    active_validator_indices = get_active_validator_indices(state.validator_registry,
    // state.slot)
    List<UInt24> active_validator_indices = get_active_validator_indices(
        state.getValidatorRegistry().toArray(new ValidatorRecord[0]), state.getSlot());
    //    # The total effective balance of active validators
    //    total_balance = sum([get_effective_balance(state, i) for i in active_validator_indices])
    UInt64 total_balance = active_validator_indices.stream()
        .map(i -> get_effective_balance(state, i))
        .reduce(UInt64::plus)
        .orElse(UInt64.ZERO);

    //    for index, validator in enumerate(state.validator_registry):
    for (int index = 0; index < state.getValidatorRegistry().size(); index++) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      //    if (state.slot // EPOCH_LENGTH) == (validator.penalized_slot // EPOCH_LENGTH)
      //        + LATEST_PENALIZED_EXIT_LENGTH // 2:
      if (state.getSlot().dividedBy(spec.getEpochLength()).equals(
          validator.getPenalizedSlot()
              .dividedBy(spec.getEpochLength())
              .plus(spec.getLatestPenalizedExitLength().dividedBy(2))
      )) {

        //    e = (state.slot // EPOCH_LENGTH) % LATEST_PENALIZED_EXIT_LENGTH
        UInt64 e = state.getSlot()
            .dividedBy(spec.getEpochLength())
            .modulo(spec.getLatestPenalizedExitLength());
        //    total_at_start = state.latest_penalized_balances[(e + 1) % LATEST_PENALIZED_EXIT_LENGTH]
        // FIXME latest_penalized_balances or latest_penalized_exit_balances
        UInt64 total_at_start = state.getLatestPenalizedExitBalances().get(
            e.increment().modulo(spec.getLatestPenalizedExitLength()).getIntValue());
        //    total_at_end = state.latest_penalized_balances[e]
        UInt64 total_at_end = state.getLatestPenalizedExitBalances().get(e.getIntValue());
        //    total_penalties = total_at_end - total_at_start
        UInt64 total_penalties = total_at_end.minus(total_at_start);
        //    penalty = get_effective_balance(state, index) *
        //        min(total_penalties * 3, total_balance) // total_balance
        UInt64 penalty = get_effective_balance(state, UInt24.valueOf(index))
            .times(UInt64s.min(total_penalties.times(3), total_balance))
            .dividedBy(total_balance);
        //    state.validator_balances[index] -= penalty
        state.withValidatorBalance(UInt24.valueOf(index), balance -> balance.minus(penalty));
      }
    }

    /*
       def eligible(index):
           validator = state.validator_registry[index]
           if validator.penalized_slot <= state.slot:
               PENALIZED_WITHDRAWAL_TIME = LATEST_PENALIZED_EXIT_LENGTH * EPOCH_LENGTH // 2
               return state.slot >= validator.penalized_slot + PENALIZED_WITHDRAWAL_TIME
           else:
               return state.slot >= validator.exit_slot + MIN_VALIDATOR_WITHDRAWAL_TIME

       all_indices = list(range(len(state.validator_registry)))
       eligible_indices = filter(eligible, all_indices)
    */
    List<UInt24> eligible_indices = new ArrayList<>();
    for (int index = 0; index < state.getValidatorRegistry().size(); index++) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getPenalizedSlot().compareTo(state.getSlot()) <= 0) {
        UInt64 PENALIZED_WITHDRAWAL_TIME = spec.getLatestPenalizedExitLength()
            .times(spec.getEpochLength())
            .dividedBy(2);
        if (state.getSlot()
            .compareTo(validator.getPenalizedSlot().plus(PENALIZED_WITHDRAWAL_TIME)) >= 0) {
          eligible_indices.add(UInt24.valueOf(index));
        }
      } else {
        if (state.getSlot()
            .compareTo(validator.getExitSlot().plus(spec.getMinValidatorWithdrawalTime())) >= 0) {
          eligible_indices.add(UInt24.valueOf(index));
        }
      }
    }

    //    sorted_indices = sorted(eligible_indices,
    //          key=lambda index: state.validator_registry[index].exit_count)
    eligible_indices.sort(Comparator.comparingLong(i ->
        state.getValidatorRegistry().get(i.getValue()).getExitCount().getValue()));
    List<UInt24> sorted_indices = eligible_indices;

    //    withdrawn_so_far = 0
    int withdrawn_so_far = 0;
    //    for index in sorted_indices:
    for (UInt24 index : sorted_indices) {
      //    prepare_validator_for_withdrawal(state, index)
      prepare_validator_for_withdrawal(state, index);
      //    withdrawn_so_far += 1
      withdrawn_so_far++;
      //    if withdrawn_so_far >= MAX_WITHDRAWALS_PER_EPOCH:
      //      break
      if (withdrawn_so_far >= spec.getMaxWithdrawalsPerEpoch().getIntValue()) {
        break;
      }
    }
  }

  public Hash32 hash_tree_root(Object object) {
    return Hash32.ZERO;
  }

  public boolean bls_verify(Bytes48 publicKey, Hash32 message, Bytes96 signature, Bytes8 domain) {
    PublicKey blsPublicKey = PublicKey.create(publicKey);
    return bls_verify(blsPublicKey, message, signature, domain);
  }

  public boolean bls_verify(
      PublicKey blsPublicKey, Hash32 message, Bytes96 signature, Bytes8 domain) {
    MessageParameters messageParameters = MessageParameters.create(message, domain);
    Signature blsSignature = Signature.create(signature);
    return BLS381.verify(messageParameters, blsSignature, blsPublicKey);
  }

  public boolean bls_verify_multiple(
      PublicKey[] publicKeys, Hash32[] messages, Bytes96 signature, Bytes8 domain) {
    assertTrue(publicKeys.length == messages.length);

    for (int i = 0; i < publicKeys.length; i++) {
      if (!bls_verify(publicKeys[i], messages[i], signature, domain)) {
        return false;
      }
    }

    return true;
  }

  public PublicKey bls_aggregate_pubkeys(List<Bytes48> publicKeysBytes) {
    List<PublicKey> publicKeys =
        publicKeysBytes.stream().map(PublicKey::create).collect(Collectors.toList());
    return PublicKey.aggregate(publicKeys);
  }

  public UInt64 get_fork_version(ForkData forkData, UInt64 slot) {
    if (slot.compareTo(forkData.getForkSlot()) < 0) {
      return forkData.getPreForkVersion();
    } else {
      return forkData.getPostForkVersion();
    }
  }

  public Bytes8 get_domain(ForkData forkData, UInt64 slot, UInt64 domainType) {
    return get_fork_version(forkData, slot).shl(32).plus(domainType).toBytes8();
  }

  public Hash32 repeat_hash(Hash32 x, int n) {
    return n == 0 ? x : repeat_hash(x, n - 1);
  }

  public List<UInt24> indices(UInt24[] custodyBit0Indices, UInt24[] custodyBit1Indices) {
    List<UInt24> indices = new ArrayList<>();
    indices.addAll(Arrays.asList(custodyBit0Indices));
    indices.addAll(Arrays.asList(custodyBit1Indices));
    return indices;
  }

  public List<UInt24> intersection(List<UInt24> indices1, List<UInt24> indices2) {
    List<UInt24> intersection = new ArrayList<>(indices1);
    intersection.retainAll(indices2);
    return intersection;
  }

  /*
   def is_double_vote(attestation_data_1: AttestationData,
                  attestation_data_2: AttestationData) -> bool
     """
     Assumes ``attestation_data_1`` is distinct from ``attestation_data_2``.
     Returns True if the provided ``AttestationData`` are slashable
     due to a 'double vote'.
     """
     target_epoch_1 = attestation_data_1.slot // EPOCH_LENGTH
     target_epoch_2 = attestation_data_2.slot // EPOCH_LENGTH
     return target_epoch_1 == target_epoch_2
  */
  public boolean is_double_vote(
      AttestationData attestation_data_1, AttestationData attestation_data_2) {
    UInt64 target_epoch_1 = attestation_data_1.getSlot(); // EPOCH_LENGTH
    UInt64 target_epoch_2 = attestation_data_2.getSlot(); // EPOCH_LENGTH
    return target_epoch_1.equals(target_epoch_2);
  }

  /*
   def is_surround_vote(attestation_data_1: AttestationData,
                      attestation_data_2: AttestationData) -> bool:
     """
     Assumes ``attestation_data_1`` is distinct from ``attestation_data_2``.
     Returns True if the provided ``AttestationData`` are slashable
     due to a 'surround vote'.
     Note: parameter order matters as this function only checks
     that ``attestation_data_1`` surrounds ``attestation_data_2``.
     """
     source_epoch_1 = attestation_data_1.justified_slot // EPOCH_LENGTH
     source_epoch_2 = attestation_data_2.justified_slot // EPOCH_LENGTH
     target_epoch_1 = attestation_data_1.slot // EPOCH_LENGTH
     target_epoch_2 = attestation_data_2.slot // EPOCH_LENGTH
     return (
         (source_epoch_1 < source_epoch_2) and
         (source_epoch_2 + 1 == target_epoch_2) and
         (target_epoch_2 < target_epoch_1)
     )
  */
  public boolean is_surround_vote(
      AttestationData attestation_data_1, AttestationData attestation_data_2) {
    UInt64 source_epoch_1 = attestation_data_1.getJustifiedSlot(); // EPOCH_LENGTH
    UInt64 source_epoch_2 = attestation_data_2.getJustifiedSlot(); // EPOCH_LENGTH
    UInt64 target_epoch_1 = attestation_data_1.getSlot(); // EPOCH_LENGTH
    UInt64 target_epoch_2 = attestation_data_2.getSlot(); // EPOCH_LENGTH

    return (source_epoch_1.compareTo(source_epoch_2) < 0)
        && (source_epoch_2.plus(1).equals(target_epoch_2))
        && (target_epoch_2.compareTo(target_epoch_1) < 0);
  }

  /*
   def verify_slashable_vote_data(state: BeaconState, vote_data: SlashableVoteData) -> bool:
     if len(vote_data.custody_bit_0_indices) + len(vote_data.custody_bit_1_indices) > MAX_CASPER_VOTES:
         return False

     return bls_verify_multiple(
         pubkeys=[
             aggregate_pubkey([state.validators[i].pubkey for i in vote_data.custody_bit_0_indices]),
             aggregate_pubkey([state.validators[i].pubkey for i in vote_data.custody_bit_1_indices]),
         ],
         messages=[
             hash_tree_root(AttestationDataAndCustodyBit(vote_data.data, False)),
             hash_tree_root(AttestationDataAndCustodyBit(vote_data.data, True)),
         ],
         signature=vote_data.aggregate_signature,
         domain=get_domain(
             state.fork_data,
             state.slot,
             DOMAIN_ATTESTATION,
         ),
     )
  */
  public boolean verify_slashable_vote_data(BeaconState state, SlashableVoteData vote_data) {
    if (vote_data.getCustodyBit0Indices().length + vote_data.getCustodyBit1Indices().length
        > spec.getMaxCasperVotes()) {
      return false;
    }

    List<Bytes48> pubKeys1 = mapIndicesToPubKeys(state, vote_data.getCustodyBit0Indices());
    List<Bytes48> pubKeys2 = mapIndicesToPubKeys(state, vote_data.getCustodyBit1Indices());

    return bls_verify_multiple(
        new PublicKey[] {bls_aggregate_pubkeys(pubKeys1), bls_aggregate_pubkeys(pubKeys2)},
        new Hash32[] {
          hash_tree_root(new AttestationDataAndCustodyBit(vote_data.getData(), false)),
          hash_tree_root(new AttestationDataAndCustodyBit(vote_data.getData(), true))
        },
        vote_data.getAggregatedSignature(),
        get_domain(state.getForkData(), state.getSlot(), ATTESTATION));
  }

  /*
   def get_block_root(state: BeaconState,
                    slot: int) -> Hash32:
     """
     Returns the block root at a recent ``slot``.
     """
     assert state.slot <= slot + LATEST_BLOCK_ROOTS_LENGTH
     assert slot < state.slot
     return state.latest_block_roots[slot % LATEST_BLOCK_ROOTS_LENGTH]
  */
  public Hash32 get_block_root(BeaconState state, UInt64 slot) {
    assertTrue(state.getSlot().compareTo(slot.plus(spec.getLatestBlockRootsLength())) <= 0);
    assertTrue(slot.compareTo(state.getSlot()) < 0);
    return state.getLatestBlockRoots().get(safeInt(slot.modulo(spec.getLatestBlockRootsLength())));
  }

  /*
   def get_attestation_participants(state: BeaconState,
                                  attestation_data: AttestationData,
                                  participation_bitfield: bytes) -> List[int]:
     """
     Returns the participant indices at for the ``attestation_data`` and ``participation_bitfield``.
     """

     # Find the committee in the list with the desired shard
     shard_committees = get_shard_committees_at_slot(state, attestation_data.slot)

     assert attestation.shard in [shard for _, shard in shard_committees]
     shard_committee = [committee for committee, shard in shard_committees if shard == attestation_data.shard][0]
     assert len(participation_bitfield) == (len(committee) + 7) // 8

     # Find the participating attesters in the committee
     participants = []
     for i, validator_index in enumerate(shard_committee):
         participation_bit = (participation_bitfield[i//8] >> (7 - (i % 8))) % 2
         if participation_bit == 1:
             participants.append(validator_index)
     return participants
  */
  public List<UInt24> get_attestation_participants(
      BeaconState state, AttestationData attestation_data, BytesValue participation_bitfield) {
    List<Pair<UInt24[], UInt64>> shard_committees =
        get_shard_committees_at_slot(state, attestation_data.getSlot());

    assertTrue(
        shard_committees.stream()
            .map(Pair::getValue1)
            .collect(Collectors.toSet())
            .contains(attestation_data.getShard()));
    Optional<Pair<UInt24[], UInt64>> shard_committee =
        shard_committees.stream()
            .filter(committee -> committee.getValue1().equals(attestation_data.getShard()))
            .findFirst();
    assertTrue(shard_committee.isPresent());
    UInt24[] committee = shard_committee.get().getValue0();
    assertTrue(participation_bitfield.size() == (committee.length + 7) / 8);

    List<UInt24> participants = new ArrayList<>();
    for (int i = 0; i < committee.length; i++) {
      UInt24 validator_index = committee[i];
      int participation_bit = (participation_bitfield.get(i / 8) & 0xFF) >> ((7 - (i % 8)) % 2);
      if (participation_bit == 1) {
        participants.add(validator_index);
      }
    }

    return participants;
  }

  public void checkIndexRange(BeaconState state, UInt24 index) {
    assertTrue(safeInt(index) < state.getValidatorRegistry().size());
  }

  public void checkIndexRange(BeaconState state, UInt24[] indices) {
    checkIndexRange(state, Arrays.asList(indices));
  }

  public void checkIndexRange(BeaconState state, List<UInt24> indices) {
    indices.forEach(index -> checkIndexRange(state, index));
  }

  public void checkShardRange(UInt64 shard) {
    assertTrue(shard.compareTo(spec.getShardCount()) < 0);
  }

  public List<Bytes48> mapIndicesToPubKeys(BeaconState state, List<UInt24> indices) {
    List<Bytes48> publicKeys = new ArrayList<>();
    for (UInt24 index : indices) {
      checkIndexRange(state, index);
      publicKeys.add(state.getValidatorRegistry().get(safeInt(index)).getPubKey());
    }
    return publicKeys;
  }

  public List<Bytes48> mapIndicesToPubKeys(BeaconState state, UInt24[] indices) {
    return mapIndicesToPubKeys(state, Arrays.asList(indices));
  }

  public static int safeInt(UInt64 uint) {
    long lVal = uint.getValue();
    assertTrue(lVal >= 0 && lVal < Integer.MAX_VALUE);
    return (int) lVal;
  }

  public static int safeInt(UInt24 uint) {
    int lVal = uint.getValue();
    assertTrue(lVal >= 0 && lVal < (1 << 24));
    return lVal;
  }

  private static void assertTrue(boolean assertion) {
    if (!assertion) {
      throw new SpecAssertionFailed();
    }
  }

  public static class SpecAssertionFailed extends RuntimeException {}
}

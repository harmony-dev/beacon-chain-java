package org.ethereum.beacon.consensus;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.crypto.Hashes;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes3;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

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
              active_validator_count // EPOCH_LENGTH // TARGET_COMMITTEE_SIZE,
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
      def get_previous_epoch_committees_per_slot(state: BeaconState) -> int:
        previous_active_validators = get_active_validator_indices(state.validator_registry, state.previous_epoch_calculation_slot)
        return get_committee_count_per_slot(len(previous_active_validators))
   */
  int get_previous_epoch_committees_per_slot(BeaconState state) {
    int[] previous_active_validators = get_active_validator_indices(
        state.getValidatorRegistry().toArray(new ValidatorRecord[0]),
        state.getPreviousEpochCalculationSlot());
    return get_committee_count_per_slot(previous_active_validators.length);
  }

  /*
    def get_current_epoch_committees_per_slot(state: BeaconState) -> int:
        current_active_validators = get_active_validator_indices(validators, state.current_epoch_calculation_slot)
        return get_committee_count_per_slot(len(current_active_validators))
   */
  int get_current_epoch_committees_per_slot(BeaconState state) {
    int[] previous_active_validators = get_active_validator_indices(
        state.getValidatorRegistry().toArray(new ValidatorRecord[0]),
        state.getCurrentEpochCalculationSlot());
    return get_committee_count_per_slot(previous_active_validators.length);
  }

  /*
    Returns the list of ``(committee, shard)`` tuples for the ``slot``.
   */
  public List<Pair<List<UInt24>, Integer>> get_shard_committees_at_slot(BeaconState state, UInt64 slot) {
    UInt64 state_epoch_slot = state.getSlot().minus(state.getSlot().modulo(spec.getEpochLength()));
    assertTrue(state_epoch_slot.compareTo(slot.plus(spec.getEpochLength())) <= 0);
    assertTrue(slot.compareTo(state_epoch_slot.plus(spec.getEpochLength())) < 0);

//    offset = slot % EPOCH_LENGTH
    UInt64 offset = slot.modulo(spec.getEpochLength());

//    if slot < state_epoch_slot:
    if (slot.compareTo(state_epoch_slot) < 0) {
//      committees_per_slot = get_previous_epoch_committees_per_slot(state)
      int committees_per_slot = get_previous_epoch_committees_per_slot(state);
//      shuffling = get_shuffling(state.previous_epoch_randao_mix,
//          state.validator_registry,
//          state.previous_epoch_calculation_slot)

      get_shuffling(state.getPreviousEpochRandaoMix(),
          state.getValidatorRegistry().toArray(new ValidatorRecord[0]),
          state.getPreviousEpochCalculationSlot());

//      slot_start_shard = (state.previous_epoch_start_shard + committees_per_slot * offset) % SHARD_COUNT
//    else:
    } else {
//      committees_per_slot = get_current_epoch_committees_per_slot(state)
//      shuffling = get_shuffling(state.current_epoch_randao_mix,
//          state.validator_registry,
//          state.current_epoch_calculation_slot)
//      slot_start_shard = (state.current_epoch_start_shard + committees_per_slot * offset) % SHARD_COUNT
    }
//    return [
//    (shuffling[committees_per_slot * offset + i], (slot_start_shard + i) % SHARD_COUNT)
//    for i in range(committees_per_slot)
//    ]
}

  /*
    first_committee = get_shard_committees_at_slot(state, slot)[0].committee
    return first_committee[slot % len(first_committee)]
   */
  public UInt24 get_beacon_proposer_index(BeaconState state, UInt64 slot) {
    List<UInt24> first_committee = get_shard_committees_at_slot(state, slot).get(0).getValue0();
    return first_committee.get(safeInt(slot.modulo(first_committee.size())));
  }


  /*
    def is_active_validator(validator: ValidatorRecord, slot: int) -> bool:
    """
    Checks if ``validator`` is active.
    """
    return validator.activation_slot <= slot < validator.exit_slot
   */
  public boolean is_active_validator(ValidatorRecord validator, UInt64 slot) {
    return validator.getActivationSlot().compareTo(slot) <= 0 &&
        slot.compareTo(validator.getExitSlot()) < 0;
  }

  /*
    def get_active_validator_indices(validators: [ValidatorRecord], slot: int) -> List[int]:
    """
    Gets indices of active validators from ``validators``.
    """
    return [i for i, v in enumerate(validators) if is_active_validator(v, slot)]
   */
  public int[] get_active_validator_indices(ValidatorRecord[] validators, UInt64 slot) {
    ArrayList<Integer> ret = new ArrayList<>();
    for (int i = 0; i < validators.length; i++) {
      if (is_active_validator(validators[i], slot)) {
        ret.add(i);
      }
    }
    return ret.stream().mapToInt(i -> i).toArray();
  }

  /*
    def shuffle(values: List[Any], seed: Hash32) -> List[Any]:
    """
    Returns the shuffled ``values`` with ``seed`` as entropy.
    """
   */
  public int[] shuffle(int[] values, Hash32 seed) {

    //    values_count = len(values)
    int values_count = values.length;

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
    int[] output = Arrays.copyOf(values, values_count);
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
          int tmp = output[index];
          output[index] = output[replacement_position];
          output[replacement_position] = tmp;
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
  public int[][] split(int[] values, int split_count) {
    int[][] ret = new int[split_count][];
    for (int i = 0; i < split_count; i++) {
      int fromIdx = values.length * i / split_count;
      int toIdx = min(values.length * (i + 1) / split_count, values.length);
      ret[i] = Arrays.copyOfRange(values, fromIdx, toIdx);
    }
    return ret;
  }

  /*
  def get_shuffling(seed: Hash32,
                validators: List[ValidatorRecord],
                crosslinking_start_shard: int,
                slot: int) -> List[List[ShardCommittee]]:
  """
  Shuffles ``validators`` into shard committees seeded by ``randao_mix`` and ``slot``.
  """
  */
  public ShardCommittee[][] get_shuffling(Hash32 seed,
                                                 ValidatorRecord[] validators,
                                                 int crosslinking_start_shard,
                                                 UInt64 _slot) {

    //     # Normalizes slot to start of epoch boundary
    //    slot -= slot % EPOCH_LENGTH
    UInt64 slot = _slot.minus(_slot.modulo(spec.getEpochLength()));

    //  active_validator_indices = get_active_validator_indices(validators, slot)
    int[] active_validator_indices = get_active_validator_indices(validators, slot);

    return get_active_shuffling(seed, active_validator_indices, crosslinking_start_shard);
  }

  ShardCommittee[][] get_active_shuffling(Hash32 seed,
                                          int[] active_validator_indices,
                                          int crosslinking_start_shard) {

    //    committees_per_slot = max(
    //        1,
    //        min(
    //            SHARD_COUNT // EPOCH_LENGTH,
    //            len(active_validator_indices) // EPOCH_LENGTH // TARGET_COMMITTEE_SIZE,
    //        )
    //    )
    int committees_per_slot = max(1,
        min(
            spec.getShardCount()
                .dividedBy(spec.getEpochLength())
                .getIntValue(),
            UInt64.valueOf(active_validator_indices.length)
                .dividedBy(spec.getEpochLength())
                .dividedBy(spec.getTargetCommitteeSize().getValue())
                .getIntValue()
        )
    );

    //    # Shuffle
    //TODO    seed = xor(randao_mix, bytes32(slot)) - looks obsolete
    //    shuffled_active_validator_indices = shuffle(active_validator_indices, seed)
    int[] shuffled_active_validator_indices = shuffle(active_validator_indices, seed);

    //    # Split the shuffled list into epoch_length pieces
    //    validators_per_slot = split(shuffled_active_validator_indices, EPOCH_LENGTH)
    int[][] validators_per_slot = split(shuffled_active_validator_indices, spec.getEpochLength().getIntValue());

    //    output = []
    List<ShardCommittee[]> output = new ArrayList<>();

    //    for slot_position, slot_indices in enumerate(validators_per_slot):
    for (int slot_position = 0; slot_position < validators_per_slot.length; slot_position++) {
      int[] slot_indices = validators_per_slot[slot_position];

      //      # Split the shuffled list into committees_per_slot pieces
      //      shard_indices = split(slot_indices, committees_per_slot)
      int[][] shard_indices = split(slot_indices, committees_per_slot);

      //      shard_id_start = crosslinking_start_shard + slot_position * committees_per_slot
      int shard_id_start = crosslinking_start_shard + slot_position * committees_per_slot;

      //      shard_committees = [
      //          ShardCommittee(
      //              shard=(shard_id_start + shard_position) % SHARD_COUNT,
      //              committee=indices,
      //              total_validator_count=len(active_validator_indices),
      //          )
      //          for shard_position, indices in enumerate(shard_indices)
      //      ]
      ShardCommittee[] shard_committees = new ShardCommittee[shard_indices.length];
      for (int shard_position = 0; shard_position < shard_indices.length; shard_position++) {
        int[] indices = shard_indices[shard_position];
        shard_committees[shard_position] = new ShardCommittee(
            UInt64.valueOf(shard_id_start + shard_position),
            Arrays.stream(indices).mapToObj(UInt24::valueOf).toArray(UInt24[]::new),
            UInt64.valueOf(active_validator_indices.length));
      }

      //      output.append(shard_committees)
      output.add(shard_committees);
    }

    return output.toArray(new ShardCommittee[0][]);
  }

  public static int safeInt(UInt64 uint) {
    long lVal = uint.getValue();
    assertTrue(lVal >= 0 && lVal < Integer.MAX_VALUE);
    return (int) lVal;
  }

  private static void assertTrue(boolean assertion) {
    if (!assertion) {
      throw new SpecAssertionFailed();
    }
  }

  public static class SpecAssertionFailed extends RuntimeException {}
}

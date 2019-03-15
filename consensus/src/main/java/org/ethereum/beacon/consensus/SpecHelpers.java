package org.ethereum.beacon.consensus;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.operations.slashing.SlashableAttestation;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Millis;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.crypto.MessageParameters;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes3;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#helper-functions
 */
public class SpecHelpers {
  private final SpecConstants constants;
  private final ObjectHasher<Hash32> objectHasher;
  private final Function<BytesValue, Hash32> hashFunction;

  /**
   * Creates a SpecHelpers instance with given {@link SpecConstants} and time supplier,
   * {@link Hashes#keccak256(BytesValue)} as a hash function and {@link SSZObjectHasher} as an object
   * hasher.
   *
   * @param constants a chain constants.
   *    <code>Schedulers::currentTime</code> is passed
   * @return spec helpers instance.
   */
  public static SpecHelpers createWithSSZHasher(@Nonnull SpecConstants constants) {
    Objects.requireNonNull(constants);

    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
    ObjectHasher<Hash32> sszHasher = SSZObjectHasher.create(hashFunction);
    return new SpecHelpers(constants, hashFunction, sszHasher);
  }

  public SpecHelpers(SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher) {
    this.constants = constants;
    this.objectHasher = objectHasher;
    this.hashFunction = hashFunction;
  }

  public SpecConstants getConstants() {
    return constants;
  }

  public Hash32 hash(BytesValue data) {
    return hashFunction.apply(data);
  }

  /*
    def get_epoch_committee_count(active_validator_count: int) -> int:
        """
        Return the number of committees in one epoch.
        """
        return max(
            1,
            min(
                SHARD_COUNT // SLOTS_PER_EPOCH,
                active_validator_count // SLOTS_PER_EPOCH // TARGET_COMMITTEE_SIZE,
            )
        ) * SLOTS_PER_EPOCH
   */
  int get_epoch_committee_count(int active_validator_count) {
    return UInt64s.max(UInt64.valueOf(1),
        UInt64s.min(
            constants.getShardCount().dividedBy(constants.getSlotsPerEpoch()),
            UInt64.valueOf(active_validator_count)
                .dividedBy(constants.getSlotsPerEpoch())
                .dividedBy(constants.getTargetCommitteeSize())
        )).times(constants.getSlotsPerEpoch()).intValue();
  }



  /*
    def get_previous_epoch_committee_count(state: BeaconState) -> int:
        """
        Return the number of committees in the previous epoch of the given ``state``.
        """
        previous_active_validators = get_active_validator_indices(
            state.validator_registry,
            state.previous_shuffling_epoch,
        )
        return get_epoch_committee_count(len(previous_active_validators))
   */
  public int get_previous_epoch_committee_count(BeaconState state) {
    List<ValidatorIndex> previous_active_validators = get_active_validator_indices(
        state.getValidatorRegistry(),
        state.getPreviousShufflingEpoch());
    return get_epoch_committee_count(previous_active_validators.size());
  }

  /*
    def get_current_epoch_committee_count(state: BeaconState) -> int:
        """
        Return the number of committees in the current epoch of the given ``state``.
        """
        current_active_validators = get_active_validator_indices(
            state.validator_registry,
            state.current_shuffling_epoch,
        )
        return get_epoch_committee_count(len(current_active_validators))
   */
  public int get_current_epoch_committee_count(BeaconState state) {
    List<ValidatorIndex> current_active_validators = get_active_validator_indices(
        state.getValidatorRegistry(),
        state.getCurrentShufflingEpoch());
    return get_epoch_committee_count(current_active_validators.size());
  }

  /*
    def get_next_epoch_committee_count(state: BeaconState) -> int:
        """
        Return the number of committees in the next epoch of the given ``state``.
        """
        next_active_validators = get_active_validator_indices(
            state.validator_registry,
            get_current_epoch(state) + 1,
        )
        return get_epoch_committee_count(len(next_active_validators))
   */
  public int get_next_epoch_committee_count(BeaconState state) {
    List<ValidatorIndex> next_active_validators = get_active_validator_indices(
        state.getValidatorRegistry(),
        get_current_epoch(state).increment());
    return get_epoch_committee_count(next_active_validators.size());
  }

  public List<ShardCommittee> get_crosslink_committees_at_slot(
      BeaconState state, SlotNumber slot) {
    return get_crosslink_committees_at_slot(state, slot, false);
  }

  /*
    Return the list of ``(committee, shard)`` tuples for the ``slot``.
    Note: There are two possible shufflings for crosslink committees for a
    ``slot`` in the next epoch -- with and without a `registry_change`
   */
  public List<ShardCommittee> get_crosslink_committees_at_slot(
      BeaconState state, SlotNumber slot, boolean registry_change) {

    EpochNumber epoch = slot_to_epoch(slot);
    EpochNumber currentEpoch = get_current_epoch(state);
    EpochNumber previousEpoch = get_previous_epoch(state);
    EpochNumber nextEpoch = currentEpoch.increment();

    assertTrue(previousEpoch.lessEqual(epoch) && epoch.lessEqual(nextEpoch));

    Hash32 seed;
    int committees_per_epoch;
    EpochNumber shuffling_epoch;
    ShardNumber shuffling_start_shard;
    // 'lookahead' might already happened if
    // get_crosslink_committees_at_slot is called after epoch transition
    // hacking around
    if ((epoch.equals(currentEpoch) && currentEpoch.greaterEqual(state.getCurrentShufflingEpoch()))
        || (epoch.equals(nextEpoch) && nextEpoch.equals(state.getCurrentShufflingEpoch()))) {
      /*
        if epoch == current_epoch:
          committees_per_epoch = get_current_epoch_committee_count(state)
          seed = state.current_shuffling_seed
          shuffling_epoch = state.current_shuffling_epoch
          shuffling_start_shard = state.current_shuffling_start_shard */

      committees_per_epoch = get_current_epoch_committee_count(state);
      seed = state.getCurrentShufflingSeed();
      shuffling_epoch = state.getCurrentShufflingEpoch();
      shuffling_start_shard = state.getCurrentShufflingStartShard();
    } else if (epoch.equals(previousEpoch) ||
        (epoch.equals(currentEpoch) && currentEpoch.less(state.getCurrentShufflingEpoch()))) {
      /*
        elif epoch == previous_epoch:
          committees_per_epoch = get_previous_epoch_committee_count(state)
          seed = state.previous_shuffling_seed
          shuffling_epoch = state.previous_shuffling_epoch
          shuffling_start_shard = state.previous_shuffling_start_shard */
      committees_per_epoch = get_previous_epoch_committee_count(state);
      seed = state.getPreviousShufflingSeed();
      shuffling_epoch = state.getPreviousShufflingEpoch();
      shuffling_start_shard = state.getPreviousShufflingStartShard();
    } else if (epoch.equals(nextEpoch)) {
      /*
        elif epoch == next_epoch:
          current_committees_per_epoch = get_current_epoch_committee_count(state)
          committees_per_epoch = get_next_epoch_committee_count(state)
          shuffling_epoch = next_epoch

          epochs_since_last_registry_update = current_epoch - state.validator_registry_update_epoch */

      int current_committees_per_epoch = get_current_epoch_committee_count(state);
      committees_per_epoch = get_next_epoch_committee_count(state);
      shuffling_epoch = nextEpoch;

      EpochNumber epochs_since_last_registry_update =
          currentEpoch.minus(state.getValidatorRegistryUpdateEpoch());

      if (registry_change) {
        /*
          if registry_change:
            seed = generate_seed(state, next_epoch)
            shuffling_start_shard = (state.current_shuffling_start_shard + current_committees_per_epoch) % SHARD_COUNT */
        seed = generate_seed(state, nextEpoch);
        shuffling_start_shard = ShardNumber.of(state.getCurrentShufflingStartShard()
            .plus(current_committees_per_epoch).modulo(constants.getShardCount()));
      } else if (epochs_since_last_registry_update.greater(EpochNumber.of(1)) &&
          is_power_of_two(epochs_since_last_registry_update)) {
        /*
          elif epochs_since_last_registry_update > 1 and is_power_of_two(epochs_since_last_registry_update):
            seed = generate_seed(state, next_epoch)
            shuffling_start_shard = state.current_shuffling_start_shard */
        seed = generate_seed(state, nextEpoch);
        shuffling_start_shard = state.getCurrentShufflingStartShard();
      } else {
        /*
          else:
            seed = state.current_shuffling_seed
            shuffling_start_shard = state.current_shuffling_start_shard */
        seed = state.getCurrentShufflingSeed();
        shuffling_start_shard = state.getCurrentShufflingStartShard();
      }
    } else {
      throw new SpecAssertionFailed();
    }

    /*
      shuffling = get_shuffling(
        seed,
        state.validator_registry,
        shuffling_epoch,
      ) */
    List<List<ValidatorIndex>> shuffling = get_shuffling2(
        seed,
        state.getValidatorRegistry(),
        shuffling_epoch
    );

    /*
      offset = slot % SLOTS_PER_EPOCH
      committees_per_slot = committees_per_epoch // SLOTS_PER_EPOCH
      slot_start_shard = (shuffling_start_shard + committees_per_slot * offset) % SHARD_COUNT */
    SlotNumber offset = slot.modulo(constants.getSlotsPerEpoch());
    UInt64 committees_per_slot = UInt64.valueOf(committees_per_epoch).dividedBy(constants.getSlotsPerEpoch());
    ShardNumber slot_start_shard = ShardNumber.of(
        shuffling_start_shard.plus(committees_per_slot).times(offset).modulo(constants.getShardCount()));

    /*
      return [
        (
            shuffling[committees_per_slot * offset + i],
            (slot_start_shard + i) % SHARD_COUNT,
        )
        for i in range(committees_per_slot)
      ] */
    List<ShardCommittee> ret = new ArrayList<>();
    for(int i = 0; i < committees_per_slot.intValue(); i++) {
      ShardCommittee committee = new ShardCommittee(
          shuffling.get(committees_per_slot.times(offset).plus(i).getIntValue()),
          slot_start_shard.plusModulo(i, constants.getShardCount()));
      ret.add(committee);
    }

    return ret;
  }

  /*
    def get_beacon_proposer_index(state: BeaconState,
                                slot: SlotNumber) -> ValidatorIndex:
      """
      Return the beacon proposer index for the ``slot``.
      """
      first_committee, _ = get_crosslink_committees_at_slot(state, slot)[0]
      return first_committee[slot % len(first_committee)]
    */
  public ValidatorIndex get_beacon_proposer_index(BeaconState state, SlotNumber slot) {
    List<ValidatorIndex> first_committee =
        get_crosslink_committees_at_slot(state, slot).get(0).getCommittee();
    return first_committee.get(slot.modulo(first_committee.size()).getIntValue());
  }

  /*
    def is_active_validator(validator: Validator, epoch: EpochNumber) -> bool:
        """
        Check if ``validator`` is active.
        """
        return validator.activation_epoch <= epoch < validator.exit_epoch
    */
  public boolean is_active_validator(ValidatorRecord validator, EpochNumber epoch) {
    return validator.getActivationEpoch().lessEqual(epoch)
        && epoch.less(validator.getExitEpoch());
  }

  /*
    def get_active_validator_indices(validators: List[Validator], epoch: EpochNumber) -> List[ValidatorIndex]:
        """
        Get indices of active validators from ``validators``.
        """
        return [i for i, v in enumerate(validators) if is_active_validator(v, epoch)]
    */
  public List<ValidatorIndex>  get_active_validator_indices(
      ReadList<ValidatorIndex, ValidatorRecord> validators, EpochNumber epochNumber) {
    ArrayList<ValidatorIndex> ret = new ArrayList<>();
    for (ValidatorIndex i : validators.size()) {
      if (is_active_validator(validators.get(i), epochNumber)) {
        ret.add(i);
      }
    }
    return ret;
  }

  /*
    def get_randao_mix(state: BeaconState,
                       epoch: EpochNumber) -> Bytes32:
        """
        Return the randao mix at a recent ``epoch``.
        """
        assert get_current_epoch(state) - LATEST_RANDAO_MIXES_LENGTH < epoch <= get_current_epoch(state)
        return state.latest_randao_mixes[epoch % LATEST_RANDAO_MIXES_LENGTH]
    */
  public Hash32 get_randao_mix(BeaconState state, EpochNumber epoch) {
    assertTrue(get_current_epoch(state).minus(constants.getLatestRandaoMixesLength()).less(epoch));
    assertTrue(epoch.lessEqual(get_current_epoch(state)));
    return state.getLatestRandaoMixes().get(
        epoch.modulo(constants.getLatestRandaoMixesLength()));
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
          //    output[index], output[replacement_position] = output[replacement_position],
          // output[index]
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
  def get_permuted_index(index: int, list_size: int, seed: Bytes32) -> int:
    """
    Return `p(index)` in a pseudorandom permutation `p` of `0...list_size-1` with ``seed`` as entropy.

    Utilizes 'swap or not' shuffling found in
    https://link.springer.com/content/pdf/10.1007%2F978-3-642-32009-5_1.pdf
    See the 'generalized domain' algorithm on page 3.
    """
    assert index < list_size
    assert list_size <= 2**40

    for round in range(SHUFFLE_ROUND_COUNT):
        pivot = bytes_to_int(hash(seed + int_to_bytes1(round))[0:8]) % list_size
        flip = (pivot - index) % list_size
        position = max(index, flip)
        source = hash(seed + int_to_bytes1(round) + int_to_bytes4(position // 256))
        byte = source[(position % 256) // 8]
        bit = (byte >> (position % 8)) % 2
        index = flip if bit else index

    return index
   */
  public UInt64 get_permuted_index(UInt64 index, UInt64 listSize, Bytes32 seed) {
    assertTrue(index.compareTo(listSize) < 0);
    assertTrue(listSize.compareTo(UInt64.valueOf(1L << 40)) <= 0);

    for (int round = 0; round < constants.getShuffleRoundCount(); round++) {
      Bytes8 pivotBytes = Bytes8.wrap(hash(seed.concat(int_to_bytes1(round))), 0);
      UInt64 pivot = bytes_to_int(pivotBytes).modulo(listSize);
      UInt64 flip = pivot.minus(index).modulo(listSize);
      UInt64 position = UInt64s.max(index, flip);
      BytesValue positionBytes = int_to_bytes4(position.dividedBy(UInt64.valueOf(256)).getValue());
      Bytes32 source = hash(seed.concat(int_to_bytes1(round)).concat(positionBytes));
      byte byt = source.get(position.modulo(256).getIntValue() / 8);
      byte bit = (byte) ((byt >> (position.modulo(8).getIntValue())) % 2);
      index = bit > 0 ? flip : index;
    }

    return index;
  }

  /**
   * An optimized version of list shuffling.
   *
   * Ported from https://github.com/protolambda/eth2-shuffle/blob/master/shuffle.go#L159
   */
  List<UInt64> get_permuted_list(List<? extends UInt64> indices, Bytes32 seed) {
    int listSize = indices.size();
    List<UInt64> permutations = new ArrayList<>(indices);

    for (int round = 0; round < constants.getShuffleRoundCount(); round++) {
      BytesValue roundSeed = seed.concat(int_to_bytes1(round));
      Bytes8 pivotBytes = Bytes8.wrap(hash(roundSeed), 0);
      int pivot = bytes_to_int(pivotBytes).modulo(listSize).getIntValue();

      int mirror = (pivot + 1) >>> 1;
      Bytes32 source = hash(roundSeed.concat(int_to_bytes4(pivot >>> 8)));

      byte byteV = source.get((pivot & 0xff) >>> 3);
      for (int i = 0, j = pivot; i < mirror; ++i, --j) {
        if ((j & 0xff) == 0xff) {
          source = hash(roundSeed.concat(int_to_bytes4(j >>> 8)));
        }
        if ((j & 0x7) == 0x7) {
          byteV = source.get((j & 0xff) >>> 3);
        }

        byte bitV = (byte) ((byteV >>> (j & 0x7)) & 0x1);
        if (bitV == 1) {
          UInt64 oldV = permutations.get(i);
          permutations.set(i, permutations.get(j));
          permutations.set(j, oldV);
        }
      }

      mirror = UInt64.valueOf(pivot).plus(listSize).increment().shr(1).getIntValue();
      int end = listSize - 1;

      source = hash(roundSeed.concat(int_to_bytes4(end >>> 8)));
      byteV = source.get((end & 0xff) >>> 3);
      for (int i = pivot + 1, j = end; i < mirror; ++i, --j) {
        if ((j & 0xff) == 0xff) {
          source = hash(roundSeed.concat(int_to_bytes4(j >>> 8)));
        }
        if ((j & 0x7) == 0x7) {
          byteV = source.get((j & 0xff) >>> 3);
        }

        byte bitV = (byte) ((byteV >>> (j & 0x7)) & 0x1);
        if (bitV == 1) {
          UInt64 oldV = permutations.get(i);
          permutations.set(i, permutations.get(j));
          permutations.set(j, oldV);
        }
      }
    }

    return permutations;
  }

  public UInt64 bytes_to_int(Bytes8 bytes) {
    return UInt64.fromBytesLittleEndian(bytes);
  }

  public BytesValue int_to_bytes1(int value) {
    return BytesValues.ofUnsignedByte(value);
  }

  public BytesValue int_to_bytes4(long value) {
    return BytesValues.ofUnsignedIntLittleEndian(value);
  }

  public BytesValue int_to_bytes32(UInt64 value) {
    return value.toBytes8LittleEndian();
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

  /*
  def get_shuffling(seed: Bytes32,
                  validators: List[Validator],
                  epoch: Epoch) -> List[List[ValidatorIndex]]
    """
    Shuffle active validators and split into crosslink committees.
    Return a list of committees (each a list of validator indices).
    """
    # Shuffle active validator indices
    active_validator_indices = get_active_validator_indices(validators, epoch)
    length = len(active_validator_indices)
    shuffled_indices = [active_validator_indices[get_permuted_index(i, length, seed)] for i in range(length)]

    # Split the shuffled active validator indices
    return split(shuffled_indices, get_epoch_committee_count(length))
   */
  public List<List<ValidatorIndex>> get_shuffling(Hash32 seed,
                               ReadList<ValidatorIndex, ValidatorRecord> validators,
                               EpochNumber epoch) {
    List<ValidatorIndex> active_validator_indices = get_active_validator_indices(validators, epoch);
    int length = active_validator_indices.size();
    List<ValidatorIndex> shuffled_indices =
        IntStream.range(0, length)
            .mapToObj(i -> get_permuted_index(UInt64.valueOf(i), UInt64.valueOf(length), seed))
            .map(permutedIndex -> active_validator_indices.get(permutedIndex.getIntValue()))
            .collect(toList());
    return split(shuffled_indices, get_epoch_committee_count(length));
  }

  /**
   * An optimized version of {@link #get_shuffling(Hash32, ReadList, EpochNumber)}.
   * Based on {@link #get_permuted_list(List, Bytes32)}.
   */
  public List<List<ValidatorIndex>> get_shuffling2(Hash32 seed,
      ReadList<ValidatorIndex, ValidatorRecord> validators,
      EpochNumber epoch) {
    List<ValidatorIndex> active_validator_indices = get_active_validator_indices(validators, epoch);
    int length = active_validator_indices.size();

    List<ValidatorIndex> shuffled_indices = get_permuted_list(active_validator_indices, seed)
        .stream().map(ValidatorIndex::new).collect(toList());

    return split(shuffled_indices, get_epoch_committee_count(length));
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

  public Hash32 merkle_root(ReadList<?, ? extends BytesValue> values) {
    return merkle_root(values.listCopy());
  }

  /*
   get_effective_balance(state: State, index: int) -> int:
     """
     Returns the effective balance (also known as "balance at stake") for a ``validator`` with the given ``index``.
     """
     return min(state.validator_balances[index], MAX_DEPOSIT * GWEI_PER_ETH)
  */
  public Gwei get_effective_balance(BeaconState state, ValidatorIndex validatorIdx) {
    return UInt64s.min(
        state.getValidatorBalances().get(validatorIdx),
        constants.getMaxDepositAmount());
  }

  /*
    def get_total_balance(state: BeaconState, validators: List[ValidatorIndex]) -> Gwei:
      """
      Return the combined effective balance of an array of validators.
      """
      return sum([get_effective_balance(state, i) for i in validators])
   */
  public Gwei get_total_balance(BeaconState state, Collection<ValidatorIndex> validators) {
    return validators.stream().map(index -> get_effective_balance(state, index))
        .reduce(Gwei.ZERO, Gwei::plus);
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
  def is_power_of_two(value: int) -> bool:
    """
    Check if ``value`` is a power of two integer.
    """
    if value == 0:
        return False
    else:
        return 2**int(math.log2(value)) == value
   */
  public boolean is_power_of_two(UInt64 value) {
    return Long.bitCount(value.getValue()) == 1;
  }

  /*
    def process_deposit(state: BeaconState, deposit: Deposit) -> None:
      """
      Process a deposit from Ethereum 1.0.
      Note that this function mutates ``state``.
      """
    */
  public void process_deposit(
      MutableBeaconState state,
      Deposit deposit) {
    process_deposit_inner(state, deposit, true);
  }
  /*
    def process_deposit(state: BeaconState, deposit: Deposit) -> None:
      """
      Process a deposit from Ethereum 1.0.
      Note that this function mutates ``state``.
      """
    */

  protected void process_deposit_inner(
      MutableBeaconState state,
      Deposit deposit,
      boolean verifyProof) {

    /* deposit_input = deposit.deposit_data.deposit_input

    proof_is_valid = bls_verify(
        pubkey=deposit_input.pubkey,
        message_hash=signed_root(deposit_input, "proof_of_possession"),
        signature=deposit_input.proof_of_possession,
        domain=get_domain(
            state.fork,
            get_current_epoch(state),
            DOMAIN_DEPOSIT,
            )
    )

    if not proof_is_valid:
      return */

    DepositInput deposit_input = deposit.getDepositData().getDepositInput();

    boolean proof_is_valid = !verifyProof ||
        bls_verify(
            deposit_input.getPubKey(),
            signed_root(deposit_input, "proofOfPossession"),
            deposit_input.getProofOfPossession(),
            get_domain(state.getForkData(), get_current_epoch(state), SignatureDomains.DEPOSIT));

    if (!proof_is_valid) {
      return;
    }

    /*
    validator_pubkeys = [v.pubkey for v in state.validator_registry]
    pubkey = deposit_input.pubkey
    amount = deposit.deposit_data.amount
    withdrawal_credentials = deposit_input.withdrawal_credentials */

    BLSPubkey pubkey = deposit_input.getPubKey();
    Gwei amount = deposit.getDepositData().getAmount();
    Hash32 withdrawal_credentials = deposit_input.getWithdrawalCredentials();
    ValidatorIndex index = get_validator_index_by_pubkey(state, pubkey);

    if (index.equals(ValidatorIndex.MAX)) {
      // Add new validator
      ValidatorRecord validator = new ValidatorRecord(
          pubkey,
          withdrawal_credentials,
          constants.getFarFutureEpoch(),
          constants.getFarFutureEpoch(),
          constants.getFarFutureEpoch(),
          Boolean.FALSE,
          Boolean.FALSE);

      // Note: In phase 2 registry indices that have been withdrawn for a long time will be
      // recycled.
      state.getValidatorRegistry().add(validator);
      state.getValidatorBalances().add(amount);
    } else {
      // Increase balance by deposit amount
      assertTrue(
          state
              .getValidatorRegistry()
              .get(index)
              .getWithdrawalCredentials()
              .equals(withdrawal_credentials));

      state.getValidatorBalances().update(index, oldBalance -> oldBalance.plus(amount));
    }
  }

  /*
    def get_delayed_activation_exit_epoch(epoch: EpochNumber) -> EpochNumber:
        """
        An entry or exit triggered in the ``epoch`` given by the input takes effect at
        the epoch given by the output.
        """
        return epoch + 1 + ACTIVATION_EXIT_DELAY
   */
  public EpochNumber get_delayed_activation_exit_epoch(EpochNumber epoch) {
    return epoch.plus(1).plus(constants.getActivationExitDelay());
  }

  /*
    def activate_validator(state: BeaconState, index: int, genesis: bool) -> None:
      validator = state.validator_registry[index]

      validator.activation_slot = GENESIS_SLOT if genesis else (state.slot + ACTIVATION_EXIT_DELAY)
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
  public void activate_validator(MutableBeaconState state, ValidatorIndex index, boolean genesis) {
    EpochNumber activationSlot =
        genesis ? constants.getGenesisEpoch() : get_delayed_activation_exit_epoch(get_current_epoch(state));
    state
        .getValidatorRegistry()
        .update(index, v -> v.builder().withActivationEpoch(activationSlot).build());
  }

  /*
    def slash_validator(state: BeaconState, index: ValidatorIndex) -> None:
    """
    Slash the validator with index ``index``.
    Note that this function mutates ``state``.
    """
    validator = state.validator_registry[index]
    assert state.slot < get_epoch_start_slot(validator.withdrawable_epoch)  # [TO BE REMOVED IN PHASE 2]
    exit_validator(state, index)
    state.latest_slashed_balances[get_current_epoch(state) % LATEST_SLASHED_EXIT_LENGTH] += get_effective_balance(state, index)

    whistleblower_index = get_beacon_proposer_index(state, state.slot)
    whistleblower_reward = get_effective_balance(state, index) // WHISTLEBLOWER_REWARD_QUOTIENT
    state.validator_balances[whistleblower_index] += whistleblower_reward
    state.validator_balances[index] -= whistleblower_reward
    validator.slashed = True
    validator.withdrawable_epoch = get_current_epoch(state) + LATEST_SLASHED_EXIT_LENGTH
    */
  public void slash_validator(MutableBeaconState state, ValidatorIndex index) {
    ValidatorRecord validator = state.getValidatorRegistry().get(index);
    assertTrue(state.getSlot().less(get_epoch_start_slot(validator.getWithdrawableEpoch())));
    exit_validator(state, index);
    state.getLatestSlashedBalances().update(
        get_current_epoch(state).modulo(constants.getLatestSlashedExitLength()),
        balance -> balance.plus(get_effective_balance(state, index)));

    ValidatorIndex whistleblower_index = get_beacon_proposer_index(state, state.getSlot());
    Gwei whistleblower_reward = get_effective_balance(state, index)
            .dividedBy(constants.getWhistleblowerRewardQuotient());
    state.getValidatorBalances().update(whistleblower_index,
        oldVal -> oldVal.plus(whistleblower_reward));
    state.getValidatorBalances().update(index,
        oldVal -> oldVal.minus(whistleblower_reward));
    state.getValidatorRegistry().update(index,
        v -> v.builder().withSlashed(Boolean.TRUE)
            .withWithdrawableEpoch(get_current_epoch(state).plus(constants.getLatestSlashedExitLength()))
            .build());
  }

  /*
   def initiate_validator_exit(state: BeaconState, index: int) -> None:
     validator = state.validator_registry[index]
     validator.initiated_exit = True
  */
  public void initiate_validator_exit(MutableBeaconState state, ValidatorIndex index) {
    state
        .getValidatorRegistry()
        .update(
            index,
            v ->
                v.builder()
                    .withInitiatedExit(Boolean.TRUE)
                    .build());
  }

  /*
    def exit_validator(state: BeaconState, index: ValidatorIndex) -> None:
      """
      Exit the validator of the given ``index``.
      Note that this function mutates ``state``.
      """
      validator = state.validator_registry[index]

      # The following updates only occur if not previous exited
      if validator.exit_epoch <= get_delayed_activation_exit_epoch(get_current_epoch(state)):
          return

      validator.exit_epoch = get_delayed_activation_exit_epoch(get_current_epoch(state))
   */
  public void exit_validator(MutableBeaconState state, ValidatorIndex index) {
    ValidatorRecord validator = state.getValidatorRegistry().get(index);
    if (validator.getExitEpoch().lessEqual(get_delayed_activation_exit_epoch(get_current_epoch(state)))) {
      return;
    }
    state.getValidatorRegistry().update(index, v ->
        v.builder().withExitEpoch(
            get_delayed_activation_exit_epoch(get_current_epoch(state))
        ).build());
  }

  /*
   def update_validator_registry(state: BeaconState) -> None:
     """
     Update validator registry.
     Note that this function mutates ``state``.
     """
  */
  public void update_validator_registry(MutableBeaconState state) {
    //     current_epoch = get_current_epoch(state)
    EpochNumber current_epoch = get_current_epoch(state);

    // # The active validators
    //    active_validator_indices = get_active_validator_indices(state.validator_registry, current_epoch)
    List<ValidatorIndex> active_validator_indices = get_active_validator_indices(
        state.getValidatorRegistry(), current_epoch);
    // # The total effective balance of active validators
    //    total_balance = get_total_balance(state, active_validator_indices)
    Gwei total_balance = get_total_balance(state, active_validator_indices);

    //    # The maximum balance churn in Gwei (for deposits and exits separately)
    //    max_balance_churn = max(
    //        MAX_DEPOSIT_AMOUNT,
    //        total_balance // (2 * MAX_BALANCE_CHURN_QUOTIENT)
    //    )
    Gwei max_balance_churn = UInt64s.max(constants.getMaxDepositAmount(),
        total_balance.dividedBy(constants.getMaxBalanceChurnQuotient().times(2)));

    //    # Activate validators within the allowable balance churn
    //    balance_churn = 0
    //    for index, validator in enumerate(state.validator_registry):
    Gwei balance_churn = Gwei.ZERO;
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      //  if validator.activation_epoch == FAR_FUTURE_EPOCH
      //     and state.validator_balances[index] >= MAX_DEPOSIT_AMOUNT:
      if (validator.getActivationEpoch().equals(constants.getFarFutureEpoch())
          && state.getValidatorBalances().get(index).greaterEqual(
              constants.getMaxDepositAmount())) {

        //    # Check the balance churn would be within the allowance
        //    balance_churn += get_effective_balance(state, index)
        balance_churn = balance_churn.plus(get_effective_balance(state, index));

        //    if balance_churn > max_balance_churn:
        //      break
        if (balance_churn.greater(max_balance_churn)) {
          break;
        }

        //    # Activate validator
        //    activate_validator(state, index, False)
        activate_validator(state, index, false);
      }
    }

    //    # Exit validators within the allowable balance churn
    //     balance_churn = 0
    balance_churn = Gwei.ZERO;
    //    for index, validator in enumerate(state.validator_registry):
    for (ValidatorIndex index : state.getValidatorRegistry().size().iterateFromZero()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      //  if validator.activation_epoch == FAR_FUTURE_EPOCH
      //      and validator.initiated_exit:
      if (validator.getActivationEpoch().equals(constants.getFarFutureEpoch())
          && validator.getInitiatedExit()) {
        //   # Check the balance churn would be within the allowance
        //   balance_churn += get_effective_balance(state, index)
        balance_churn = balance_churn.plus(get_effective_balance(state, index));
        //   if balance_churn > max_balance_churn:
        //       break
        if (balance_churn.greater(max_balance_churn)) {
          break;
        }
        //   # Exit validator
        //   exit_validator(state, index)
        exit_validator(state, index);
      }
    }

    //    state.validator_registry_update_slot = state.slot
    //  FIXME check field name
    state.setValidatorRegistryUpdateEpoch(current_epoch);
  }

  /*
   def prepare_validator_for_withdrawal(state: BeaconState, index: ValidatorIndex) -> None:
      """
      Set the validator with the given ``index`` as withdrawable
      ``MIN_VALIDATOR_WITHDRAWABILITY_DELAY`` after the current epoch.
      Note that this function mutates ``state``.
      """
      validator = state.validator_registry[index]
      validator.withdrawable_epoch = get_current_epoch(state) + MIN_VALIDATOR_WITHDRAWABILITY_DELAY
  */
  public void prepare_validator_for_withdrawal(MutableBeaconState state, ValidatorIndex index) {
    state
        .getValidatorRegistry()
        .update(
            index,
            v ->
                v.builder()
                    .withWithdrawableEpoch(
                        get_current_epoch(state).plus(constants.getMinValidatorWithdrawabilityDelay()))
                    .build());
  }

  /*
      Process the slashings.
      Note that this function mutates ``state``.
  */
  public void process_slashings(MutableBeaconState state) {
    // current_epoch = get_current_epoch(state)
    EpochNumber current_epoch = get_current_epoch(state);

    //  # The active validators
    //  active_validator_indices = get_active_validator_indices(state.validator_registry, current_epoch)
    List<ValidatorIndex> active_validator_indices = get_active_validator_indices(
        state.getValidatorRegistry(), current_epoch);
    //    # The total effective balance of active validators
    //    total_balance = sum([get_effective_balance(state, i) for i in active_validator_indices])
    Gwei total_balance =
        active_validator_indices.stream()
            .map(i -> get_effective_balance(state, i))
            .reduce(Gwei::plus)
            .orElse(Gwei.ZERO);

    //    for index, validator in enumerate(state.validator_registry):
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      //  if validator.slashed and current_epoch ==
      //  validator.withdrawable_epoch - LATEST_SLASHED_EXIT_LENGTH // 2:
      if (validator.getSlashed() && current_epoch.equals(
          validator.getWithdrawableEpoch().minus(constants.getLatestSlashedExitLength().half()))) {

        //  epoch_index = current_epoch % LATEST_SLASHED_EXIT_LENGTH
        EpochNumber epoch_index = current_epoch.modulo(constants.getLatestSlashedExitLength());
        // total_at_start = state.latest_slashed_balances[(epoch_index + 1) % LATEST_SLASHED_EXIT_LENGTH]
        Gwei total_at_start = state.getLatestSlashedBalances().get(
            epoch_index.increment().modulo(constants.getLatestSlashedExitLength()));
        //  total_at_end = state.latest_slashed_balances[epoch_index]
        Gwei total_at_end = state.getLatestSlashedBalances().get(epoch_index);
        //    total_penalties = total_at_end - total_at_start
        Gwei total_penalties = total_at_end.minus(total_at_start);
        //    penalty = max(
        //      get_effective_balance(state, index) * min(total_penalties * 3, total_balance) // total_balance,
        //      get_effective_balance(state, index) // MIN_PENALTY_QUOTIENT
        //    )
        Gwei penalty =
            get_effective_balance(state, index)
                .mulDiv(UInt64s.min(total_penalties.times(3), total_balance), total_balance);
        //    state.validator_balances[index] -= penalty
        state.getValidatorBalances().update(index, balance -> balance.minus(penalty));
      }
    }
  }

  /*
      Process the exit queue.
      Note that this function mutates ``state``.
  */
  public void process_exit_queue(MutableBeaconState state) {

    /*
       def eligible(index):
        validator = state.validator_registry[index]
        # Filter out dequeued validators
        if validator.withdrawable_epoch != FAR_FUTURE_EPOCH:
            return False
        # Dequeue if the minimum amount of time has passed
        else:
            return get_current_epoch(state) >= validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY

       eligible_indices = filter(eligible, list(range(len(state.validator_registry))))
    */
    List<ValidatorIndex> eligible_indices = new ArrayList<>();
    for (ValidatorIndex index : state.getValidatorRegistry().size().iterateFromZero()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getWithdrawableEpoch().equals(constants.getFarFutureEpoch())) {
        eligible_indices.add(index);
      } else {
        if (get_current_epoch(state).greaterEqual(
            validator.getExitEpoch().plus(constants.getMinValidatorWithdrawabilityDelay()))) {
          eligible_indices.add(index);
        }
      }
    }

    // # Sort in order of exit epoch, and validators that exit within the same epoch exit in order of validator index
    //  sorted_indices = sorted(eligible_indices,
    //          key=lambda index: state.validator_registry[index].exit_epoch)
    eligible_indices.sort(Comparator.comparing(i ->
        state.getValidatorRegistry().get(i).getExitEpoch()));
    List<ValidatorIndex> sorted_indices = eligible_indices;

    // for dequeues, index in enumerate(sorted_indices):
    //        if dequeues >= MAX_EXIT_DEQUEUES_PER_EPOCH:
    //            break
    //        prepare_validator_for_withdrawal(state, index)

    //    withdrawn_so_far = 0
    int withdrawn_so_far = 0;
    //    for index in sorted_indices:
    for (ValidatorIndex index : sorted_indices) {
      //    prepare_validator_for_withdrawal(state, index)
      prepare_validator_for_withdrawal(state, index);
      //    withdrawn_so_far += 1
      withdrawn_so_far++;
      //    if withdrawn_so_far >= MAX_EXIT_DEQUEUES_PER_EPOCH:
      //      break
      if (withdrawn_so_far >= constants.getMaxExitDequesPerEpoch().getIntValue()) {
        break;
      }
    }
  }

  /** Function for hashing objects into a single root utilizing a hash tree structure */
  public Hash32 hash_tree_root(Object object) {
    return objectHasher.getHash(object);
  }

  /** Function for hashing objects with part starting from field rejected */
  public Hash32 signed_root(Object object, String field) {
    return objectHasher.getHashTruncate(object, field);
  }

  /*
    def get_active_index_root(state: BeaconState,
                              epoch: EpochNumber) -> Bytes32:
        """
        Return the index root at a recent ``epoch``.
        """
        assert get_current_epoch(state) - LATEST_ACTIVE_INDEX_ROOTS_LENGTH + ACTIVATION_EXIT_DELAY < epoch
            <= get_current_epoch(state) + ACTIVATION_EXIT_DELAY
        return state.latest_active_index_roots[epoch % LATEST_ACTIVE_INDEX_ROOTS_LENGTH]
   */
  Hash32 get_active_index_root(BeaconState state, EpochNumber epoch) {
    assertTrue(get_current_epoch(state).minus(constants.getLatestActiveIndexRootsLength()).plus(
        constants.getActivationExitDelay())
        .less(epoch));
    assertTrue(epoch.lessEqual(get_current_epoch(state).plus(constants.getActivationExitDelay())));
    return state.getLatestActiveIndexRoots().get(epoch.modulo(constants.getLatestActiveIndexRootsLength()));
  }

  /*
    def generate_seed(state: BeaconState,
                  epoch: Epoch) -> Bytes32:
      """
      Generate a seed for the given ``epoch``.
      """
      return hash(
          get_randao_mix(state, epoch - MIN_SEED_LOOKAHEAD) +
          get_active_index_root(state, epoch) +
          int_to_bytes32(epoch)
      )
   */
  public Hash32 generate_seed(BeaconState state, EpochNumber epoch) {
    return hash(
        get_randao_mix(state, epoch.minus(constants.getMinSeedLookahead()))
            .concat(get_active_index_root(state, epoch))
            .concat(int_to_bytes32(epoch)));
  }

  public boolean bls_verify(BLSPubkey publicKey, Hash32 message, BLSSignature signature, Bytes8 domain) {
    PublicKey blsPublicKey = PublicKey.create(publicKey);
    return bls_verify(blsPublicKey, message, signature, domain);
  }

  public boolean bls_verify(
      PublicKey blsPublicKey, Hash32 message, BLSSignature signature, Bytes8 domain) {
    MessageParameters messageParameters = MessageParameters.create(message, domain);
    Signature blsSignature = Signature.create(signature);
    return BLS381.verify(messageParameters, blsSignature, blsPublicKey);
  }

  public boolean bls_verify_multiple(
      List<PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, Bytes8 domain) {
    List<MessageParameters> messageParameters =
        messages.stream()
            .map(hash -> MessageParameters.create(hash, domain))
            .collect(Collectors.toList());
    Signature blsSignature = Signature.create(signature);
    return BLS381.verifyMultiple(messageParameters, blsSignature, publicKeys);
  }

  public PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
    List<PublicKey> publicKeys = publicKeysBytes.stream().map(PublicKey::create).collect(toList());
    return PublicKey.aggregate(publicKeys);
  }

  /*
    def get_fork_version(fork: Fork,
                     epoch: EpochNumber) -> int:
    """
    Return the fork version of the given ``epoch``.
    """
    if epoch < fork.epoch:
        return fork.previous_version
    else:
        return fork.current_version
   */
  public UInt64 get_fork_version(ForkData forkData, EpochNumber epoch) {
    if (epoch.less(forkData.getEpoch())) {
      return forkData.getPreviousVersion();
    } else {
      return forkData.getCurrentVersion();
    }
  }

  /*
    def get_domain(fork: Fork,
               epoch: EpochNumber,
               domain_type: int) -> int:
    """
    Get the domain number that represents the fork meta and signature domain.
    """
    fork_version = get_fork_version(fork, epoch)
    return fork_version * 2**32 + domain_type
   */
  public Bytes8 get_domain(ForkData forkData, EpochNumber epoch, UInt64 domainType) {
    return get_fork_version(forkData, epoch).shl(32).plus(domainType).toBytes8();
  }

  /*
   def is_double_vote(attestation_data_1: AttestationData,
                  attestation_data_2: AttestationData) -> bool
     """
     Assumes ``attestation_data_1`` is distinct from ``attestation_data_2``.
     Returns True if the provided ``AttestationData`` are slashable
     due to a 'double vote'.
     """
     target_epoch_1 = attestation_data_1.slot // SLOTS_PER_EPOCH
     target_epoch_2 = attestation_data_2.slot // SLOTS_PER_EPOCH
     return target_epoch_1 == target_epoch_2
  */
  public boolean is_double_vote(
      AttestationData attestation_data_1, AttestationData attestation_data_2) {
    EpochNumber target_epoch_1 = slot_to_epoch(attestation_data_1.getSlot());
    EpochNumber target_epoch_2 = slot_to_epoch(attestation_data_2.getSlot());
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
     source_epoch_1 = attestation_data_1.justified_slot // SLOTS_PER_EPOCH
     source_epoch_2 = attestation_data_2.justified_slot // SLOTS_PER_EPOCH
     target_epoch_1 = attestation_data_1.slot // SLOTS_PER_EPOCH
     target_epoch_2 = attestation_data_2.slot // SLOTS_PER_EPOCH
     return (
         (source_epoch_1 < source_epoch_2) and
         (source_epoch_2 + 1 == target_epoch_2) and
         (target_epoch_2 < target_epoch_1)
     )
  */
  public boolean is_surround_vote(
      AttestationData attestation_data_1, AttestationData attestation_data_2) {
    EpochNumber source_epoch_1 = attestation_data_1.getJustifiedEpoch();
    EpochNumber source_epoch_2 = attestation_data_2.getJustifiedEpoch();
    EpochNumber target_epoch_1 = slot_to_epoch(attestation_data_1.getSlot());
    EpochNumber target_epoch_2 = slot_to_epoch(attestation_data_2.getSlot());

    return (source_epoch_1.less(source_epoch_2))
        && (target_epoch_2.less(target_epoch_1));
  }


  /*
    def verify_slashable_attestation(state: BeaconState, slashable_attestation: SlashableAttestation) -> bool:
      """
      Verify validity of ``slashable_attestation`` fields.
      """
   */
  public boolean verify_slashable_attestation(BeaconState state, SlashableAttestation slashable_attestation) {
    //  if slashable_attestation.custody_bitfield != b'\x00' * len(slashable_attestation.custody_bitfield):  # [TO BE REMOVED IN PHASE 1]
    //    return False
    if (!slashable_attestation.getCustodyBitfield().isZero()) return false;

    //  if len(slashable_attestation.validator_indices) == 0:
    //  return False
    if (slashable_attestation.getValidatorIndices().size() == 0) return false;

    //  for i in range(len(slashable_attestation.validator_indices) - 1):
    //    if slashable_attestation.validator_indices[i] >= slashable_attestation.validator_indices[i + 1]:
    //      return False

    for (int i = 0; i < slashable_attestation.getValidatorIndices().size() - 1; i++) {
      if (slashable_attestation.getValidatorIndices().get(i).greaterEqual(
          slashable_attestation.getValidatorIndices().get(i + 1))) {
        return false;
      }
    }

    //  if not verify_bitfield(slashable_attestation.custody_bitfield, len(slashable_attestation.validator_indices)):
    //  return False
    if (!verify_bitfield(slashable_attestation.getCustodyBitfield(), slashable_attestation.getValidatorIndices().size())) {
      return false;
    }

    //  if len(slashable_attestation.validator_indices) > MAX_INDICES_PER_SLASHABLE_VOTE:
    //  return False
    if (UInt64.valueOf(slashable_attestation.getValidatorIndices().size()).
        compareTo(constants.getMaxIndicesPerSlashableVote()) > 0) {
      return false;
    }

    /*
      custody_bit_0_indices = []
      custody_bit_1_indices = []
      for i, validator_index in enumerate(slashable_attestation.validator_indices):
          if get_bitfield_bit(slashable_attestation.custody_bitfield, i) == 0b0:
              custody_bit_0_indices.append(validator_index)
          else:
              custody_bit_1_indices.append(validator_index)
    */
    List<ValidatorIndex> custody_bit_0_indices = new ArrayList<>();
    List<ValidatorIndex> custody_bit_1_indices = new ArrayList<>();
    for (int i = 0; i < slashable_attestation.getValidatorIndices().size(); i++) {
      ValidatorIndex validator_index = slashable_attestation.getValidatorIndices().get(i);
      if (slashable_attestation.getCustodyBitfield().getBit(i) == false) {
        custody_bit_0_indices.add(validator_index);
      } else {
        custody_bit_1_indices.add(validator_index);
      }
    }

    /*
      return bls_verify(
          pubkeys=[
              bls_aggregate_pubkeys([state.validator_registry[i].pubkey for i in custody_bit_0_indices]),
              bls_aggregate_pubkeys([state.validator_registry[i].pubkey for i in custody_bit_1_indices]),
          ],
          messages=[
              hash_tree_root(AttestationDataAndCustodyBit(data=slashable_attestation.data, custody_bit=0b0)),
              hash_tree_root(AttestationDataAndCustodyBit(data=slashable_attestation.data, custody_bit=0b1)),
          ],
          signature=slashable_attestation.aggregate_signature,
          domain=get_domain(
              state.fork,
              slot_to_epoch(vote_data.data.slot),
              DOMAIN_ATTESTATION,
          ),
      )
    */
    List<BLSPubkey> pubKeys1 = mapIndicesToPubKeys(state, custody_bit_0_indices);
    List<BLSPubkey> pubKeys2 = mapIndicesToPubKeys(state, custody_bit_1_indices);

    return bls_verify_multiple(
        Arrays.asList(bls_aggregate_pubkeys(pubKeys1), bls_aggregate_pubkeys(pubKeys2)),
        Arrays.asList(
            hash_tree_root(new AttestationDataAndCustodyBit(slashable_attestation.getData(), false)),
            hash_tree_root(new AttestationDataAndCustodyBit(slashable_attestation.getData(), true))),
        slashable_attestation.getBlsSignature(),
        get_domain(state.getForkData(), slot_to_epoch(slashable_attestation.getData().getSlot()), ATTESTATION));
  }

  /*
  def verify_bitfield(bitfield: bytes, committee_size: int) -> bool:
    """
    Verify ``bitfield`` against the ``committee_size``.
    """
    if len(bitfield) != (committee_size + 7) // 8:
        return False

    for i in range(committee_size, len(bitfield) * 8):
        if get_bitfield_bit(bitfield, i) == 0b1:
            return False

    return True
   */
  boolean verify_bitfield(Bitfield bitfield, int committee_size) {
    if (bitfield.size() != (committee_size + 7) / 8) {
      return false;
    }

    for(int i = committee_size; i < bitfield.size() * 8; i++) {
      try {
        if (bitfield.getBit(i)) {
          return false;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return true;
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
  public Hash32 get_block_root(BeaconState state, SlotNumber slot) {
    assertTrue(state.getSlot().lessEqual(slot.plus(constants.getLatestBlockRootsLength())));
    assertTrue(slot.less(state.getSlot()));
    return state.getLatestBlockRoots().get(slot.modulo(constants.getLatestBlockRootsLength()));
  }

  /*
    def get_attestation_participants(state: BeaconState,
                                     attestation_data: AttestationData,
                                     bitfield: bytes) -> List[ValidatorIndex]:
      """
      Return the participant indices at for the ``attestation_data`` and ``bitfield``.
      """
      # Find the committee in the list with the desired shard
      crosslink_committees = get_crosslink_committees_at_slot(state, attestation_data.slot)

      assert attestation_data.shard in [shard for _, shard in crosslink_committees]
      crosslink_committee = [committee for committee, shard in crosslink_committees if shard == attestation_data.shard][0]

      assert verify_bitfield(bitfield, len(crosslink_committee))

      # Find the participating attesters in the committee
      participants = []
      for i, validator_index in enumerate(crosslink_committee):
          aggregation_bit = get_bitfield_bit(bitfield, i)
          if aggregation_bit == 0b1:
              participants.append(validator_index)
      return participants
   */
  public List<ValidatorIndex> get_attestation_participants(
      BeaconState state, AttestationData attestation_data, Bitfield bitfield) {
    List<ShardCommittee> crosslink_committees =
        get_crosslink_committees_at_slot(state, attestation_data.getSlot());

    assertTrue(crosslink_committees.stream()
            .anyMatch(cc -> attestation_data.getShard().equals(cc.getShard())));
    Optional<ShardCommittee> crosslink_committee_opt =
        crosslink_committees.stream()
            .filter(committee -> committee.getShard().equals(attestation_data.getShard()))
            .findFirst();
    assertTrue(crosslink_committee_opt.isPresent());
    List<ValidatorIndex> crosslink_committee = crosslink_committee_opt.get().getCommittee();
    assertTrue(verify_bitfield(bitfield, crosslink_committee.size()));

    List<ValidatorIndex> participants = new ArrayList<>();
    for (int i = 0; i < crosslink_committee.size(); i++) {
      ValidatorIndex validator_index = crosslink_committee.get(i);
      boolean aggregation_bit = bitfield.getBit(i);
      if (aggregation_bit) {
        participants.add(validator_index);
      }
    }

    return participants;
  }

  /*
   def verify_merkle_branch(leaf: Bytes32, branch: [Bytes32], depth: int, index: int, root: Bytes32) -> bool:
     value = leaf
     for i in range(depth):
         if index // (2**i) % 2:
             value = hash(branch[i] + value)
         else:
             value = hash(value + branch[i])
     return value == root
  */
  public boolean verify_merkle_branch(
      Hash32 leaf, List<Hash32> branch, UInt64 depth, UInt64 index, Hash32 root) {

    Hash32 value = leaf;
    for (int i : IntStream.range(0, depth.intValue()).toArray()) {
      if (index.dividedBy(UInt64.valueOf(1 << i)).modulo(UInt64.valueOf(2)).compareTo(UInt64.ZERO)
          > 0) {
        value = hash(branch.get(i).concat(value));
      } else {
        value = hash(value.concat(branch.get(i)));
      }
    }

    return value.equals(root);
  }

  public ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    ValidatorIndex index = ValidatorIndex.MAX;
    for (ValidatorIndex i : state.getValidatorRegistry().size()) {
      if (state.getValidatorRegistry().get(i).getPubKey().equals(pubkey)) {
        index = i;
        break;
      }
    }

    return index;
  }

  public SlotNumber get_current_slot(BeaconState state, long systemTime) {
    Millis currentTime = Millis.of(systemTime);
    assertTrue(state.getGenesisTime().lessEqual(currentTime.getSeconds()));
    Time sinceGenesis = currentTime.getSeconds().minus(state.getGenesisTime());
    return SlotNumber.castFrom(sinceGenesis.dividedBy(constants.getSecondsPerSlot()))
        .plus(getConstants().getGenesisSlot());
  }

  public boolean is_current_slot(BeaconState state, long systemTime) {
    return state.getSlot().equals(get_current_slot(state, systemTime));
  }

  public Time get_slot_start_time(BeaconState state, SlotNumber slot) {
    return state
        .getGenesisTime()
        .plus(constants.getSecondsPerSlot().times(slot.minus(getConstants().getGenesisSlot())));
  }

  public Time get_slot_middle_time(BeaconState state, SlotNumber slot) {
    return get_slot_start_time(state, slot).plus(constants.getSecondsPerSlot().dividedBy(2));
  }

  /*
   def slot_to_epoch(slot: SlotNumber) -> EpochNumber:
       return slot // SLOTS_PER_EPOCH
  */
  public EpochNumber slot_to_epoch(SlotNumber slot) {
    return slot.dividedBy(constants.getSlotsPerEpoch());
  }

  /*
  def get_previous_epoch(state: BeaconState) -> Epoch:
    """`
    Return the previous epoch of the given ``state``.
    """
    return max(get_current_epoch(state) - 1, GENESIS_EPOCH)
   */
  public EpochNumber get_previous_epoch(BeaconState state) {
    return UInt64s.max(get_current_epoch(state).decrement(), constants.getGenesisEpoch());
  }

  /*
   def get_current_epoch(state: BeaconState) -> EpochNumber:
       return slot_to_epoch(state.slot)
  */
  public EpochNumber get_current_epoch(BeaconState state) {
    return slot_to_epoch(state.getSlot());
  }
  /*
   def get_epoch_start_slot(epoch: EpochNumber) -> SlotNumber:
     return epoch * SLOTS_PER_EPOCH
  */
  public SlotNumber get_epoch_start_slot(EpochNumber epoch) {
    return epoch.mul(constants.getSlotsPerEpoch());
  }

  public EpochNumber get_genesis_epoch() {
    return slot_to_epoch(constants.getGenesisSlot());
  }

  public void checkIndexRange(BeaconState state, ValidatorIndex index) {
    assertTrue(index.less(state.getValidatorRegistry().size()));
  }

  public void checkIndexRange(BeaconState state, Iterable<ValidatorIndex> indices) {
    indices.forEach(index -> checkIndexRange(state, index));
  }

  public void checkShardRange(ShardNumber shard) {
    assertTrue(shard.less(constants.getShardCount()));
  }

  public List<BLSPubkey> mapIndicesToPubKeys(BeaconState state, Iterable<ValidatorIndex> indices) {
    List<BLSPubkey> publicKeys = new ArrayList<>();
    for (ValidatorIndex index : indices) {
      checkIndexRange(state, index);
      publicKeys.add(state.getValidatorRegistry().get(index).getPubKey());
    }
    return publicKeys;
  }

  /*
    def lmd_ghost(store: Store, start_state: BeaconState, start_block: BeaconBlock) -> BeaconBlock:
      """
      Execute the LMD-GHOST algorithm to find the head ``BeaconBlock``.
      """
      validators = start_state.validator_registry
      active_validators = [
          validators[i]
          for i in get_active_validator_indices(validators, start_state.slot)
      ]
      attestation_targets = [
          get_latest_attestation_target(store, validator)
          for validator in active_validators
      ]

      def get_vote_count(block: BeaconBlock) -> int:
          return len([
              target
              for target in attestation_targets
              if get_ancestor(store, target, block.slot) == block
          ])

      head = start_block
      while 1:
          children = get_children(store, head)
          if len(children) == 0:
              return head
          head = max(children, key=get_vote_count)
   */
  // FIXME should be epoch parameter get_active_validator_indices(validators, start_state.slot)
  public BeaconBlock lmd_ghost(
      BeaconBlock startBlock,
      BeaconState state,
      Function<Hash32, Optional<BeaconBlock>> getBlock,
      Function<Hash32, List<BeaconBlock>> getChildrenBlocks,
      Function<ValidatorRecord, Optional<Attestation>> get_latest_attestation) {
    ReadList<ValidatorIndex, ValidatorRecord> validators = state.getValidatorRegistry();
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(validators, get_current_epoch(state));

    List<Pair<ValidatorIndex, ValidatorRecord>> active_validators = new ArrayList<>();
    for (ValidatorIndex index : active_validator_indices) {
      active_validators.add(Pair.with(index, validators.get(index)));
    }

    List<Pair<ValidatorIndex, BeaconBlock>> attestation_targets = new ArrayList<>();
    for (Pair<ValidatorIndex, ValidatorRecord> validatorRecord : active_validators) {
      get_latest_attestation_target(validatorRecord.getValue1(), get_latest_attestation, getBlock)
          .ifPresent(block -> attestation_targets.add(Pair.with(validatorRecord.getValue0(), block)));
    }

    BeaconBlock head = startBlock;
    while (true) {
      List<BeaconBlock> children = getChildrenBlocks.apply(hash_tree_root(head));
      if (children.isEmpty()) {
        return head;
      } else {
        head =
            children.stream()
                .max(
                    Comparator.comparing(o -> get_vote_count(state, o, attestation_targets, getBlock),
                    UInt64::compareTo))
                .get();
      }
    }
  }

  /**
   * Let get_latest_attestation_target(store, validator) be the target block in the attestation
   * get_latest_attestation(store, validator).
   *
   * @param get_latest_attestation Let get_latest_attestation(store, validator) be the attestation
   *     with the highest slot number in store from validator. If several such attestations exist,
   *     use the one the validator v observed first.
   */
  private Optional<BeaconBlock> get_latest_attestation_target(
      ValidatorRecord validatorRecord,
      Function<ValidatorRecord, Optional<Attestation>> get_latest_attestation,
      Function<Hash32, Optional<BeaconBlock>> getBlock) {
    Optional<Attestation> latest = get_latest_attestation.apply(validatorRecord);
    return latest.flatMap(at -> getBlock.apply(at.getData().getJustifiedBlockRoot()));
  }

  /*
    def get_vote_count(block: BeaconBlock) -> int:
      return sum(
          get_effective_balance(start_state.validator_balances[validator_index]) // FORK_CHOICE_BALANCE_INCREMENT
          for validator_index, target in attestation_targets
          if get_ancestor(store, target, block.slot) == block
      )
   */
  private UInt64 get_vote_count(
      BeaconState startState,
      BeaconBlock block,
      List<Pair<ValidatorIndex, BeaconBlock>> attestation_targets,
      Function<Hash32, Optional<BeaconBlock>> getBlock) {

    return attestation_targets.stream().filter(
        target -> get_ancestor(target.getValue1(), block.getSlot(), getBlock)
            .filter(ancestor -> ancestor.equals(block)).isPresent())
        .map(target -> get_effective_balance(startState, target.getValue0()).dividedBy(constants.getForkChoiceBalanceIncrement()))
        .reduce(Gwei.ZERO, Gwei::plus);
  }

  /*
    def get_ancestor(store: Store, block: BeaconBlock, slot: SlotNumber) -> BeaconBlock:
      """
      Get the ancestor of ``block`` with slot number ``slot``; return ``None`` if not found.
      """
      if block.slot == slot:
          return block
      elif block.slot < slot:
          return None
      else:
          return get_ancestor(store, store.get_parent(block), slot)
   */
  private Optional<BeaconBlock> get_ancestor(
      BeaconBlock block, SlotNumber slot, Function<Hash32, Optional<BeaconBlock>> getBlock) {
    if (block.getSlot().equals(slot)) {
      return Optional.of(block);
    } else if (block.getSlot().less(slot)) {
      return Optional.empty();
    } else {
      return getBlock
          .apply(block.getParentRoot())
          .flatMap(parent -> get_ancestor(parent, slot, getBlock));
    }
  }

  public boolean is_epoch_end(SlotNumber slot) {
    return slot.increment().modulo(constants.getSlotsPerEpoch()).equals(SlotNumber.ZERO);
  }

  public ObjectHasher<Hash32> getObjectHasher() {
    return objectHasher;
  }

  public Function<BytesValue, Hash32> getHashFunction() {
    return hashFunction;
  }

  private static void assertTrue(boolean assertion) {
    if (!assertion) {
      throw new SpecAssertionFailed();
    }
  }

  public static class SpecAssertionFailed extends RuntimeException {}
}

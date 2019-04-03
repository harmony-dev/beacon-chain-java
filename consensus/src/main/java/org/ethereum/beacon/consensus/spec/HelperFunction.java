package org.ethereum.beacon.consensus.spec;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.operations.slashing.SlashableAttestation;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * Helper functions.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.1/specs/core/0_beacon-chain.md#helper-functions">Helper
 *     functions</a> in ths spec.
 */
public interface HelperFunction extends SpecCommons {

  default Hash32 hash(BytesValue data) {
    return getHashFunction().apply(data);
  }

  /*
    def xor(bytes1: Bytes32, bytes2: Bytes32) -> Bytes32:
      return bytes(a ^ b for a, b in zip(bytes1, bytes2))
   */
  default Bytes32 xor(Bytes32 bytes1, Bytes32 bytes2) {
    return Bytes32s.xor(bytes1, bytes2);
  }

  /*
   def get_temporary_block_header(block: BeaconBlock) -> BeaconBlockHeader:
     """
     Return the block header corresponding to a block with ``state_root`` set to ``ZERO_HASH``.
     """
     return BeaconBlockHeader(
         slot=block.slot,
         previous_block_root=block.previous_block_root,
         state_root=ZERO_HASH,
         block_body_root=hash_tree_root(block.body),
         # signed_root(block) is used for block id purposes so signature is a stub
         signature=EMPTY_SIGNATURE,
     )
  */
  default BeaconBlockHeader get_temporary_block_header(BeaconBlock block) {
    return new BeaconBlockHeader(
        block.getSlot(),
        block.getPreviousBlockRoot(),
        Hash32.ZERO,
        hash_tree_root(block.getBody()),
        getConstants().getEmptySignature());
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
  default int get_epoch_committee_count(int active_validator_count) {
    return UInt64s.max(UInt64.valueOf(1),
        UInt64s.min(
            getConstants().getShardCount().dividedBy(getConstants().getSlotsPerEpoch()),
            UInt64.valueOf(active_validator_count)
                .dividedBy(getConstants().getSlotsPerEpoch())
                .dividedBy(getConstants().getTargetCommitteeSize())
        )).times(getConstants().getSlotsPerEpoch()).intValue();
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
  default int get_previous_epoch_committee_count(BeaconState state) {
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
  default int get_current_epoch_committee_count(BeaconState state) {
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
  default int get_next_epoch_committee_count(BeaconState state) {
    List<ValidatorIndex> next_active_validators = get_active_validator_indices(
        state.getValidatorRegistry(),
        get_current_epoch(state).increment());
    return get_epoch_committee_count(next_active_validators.size());
  }

  default List<ShardCommittee> get_crosslink_committees_at_slot(
      BeaconState state, SlotNumber slot) {
    return get_crosslink_committees_at_slot(state, slot, false);
  }

  /*
    Return the list of ``(committee, shard)`` tuples for the ``slot``.
    Note: There are two possible shufflings for crosslink committees for a
    ``slot`` in the next epoch -- with and without a `registry_change`
   */
  default List<ShardCommittee> get_crosslink_committees_at_slot(
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

    if (epoch.equals(currentEpoch)) {
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
    } else if (epoch.equals(previousEpoch)) {
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
          epochs_since_last_registry_update = current_epoch - state.validator_registry_update_epoch */

      EpochNumber epochs_since_last_registry_update =
          currentEpoch.minus(state.getValidatorRegistryUpdateEpoch());

      if (registry_change) {
        /*
        if registry_change:
            committees_per_epoch = get_next_epoch_committee_count(state)
            seed = generate_seed(state, next_epoch)
            shuffling_epoch = next_epoch
            current_committees_per_epoch = get_current_epoch_committee_count(state)
            shuffling_start_shard = (state.current_shuffling_start_shard + current_committees_per_epoch) % SHARD_COUNT */

        committees_per_epoch = get_next_epoch_committee_count(state);
        seed = generate_seed(state, nextEpoch);
        shuffling_epoch = nextEpoch;
        int current_committees_per_epoch = get_current_epoch_committee_count(state);
        shuffling_start_shard = ShardNumber.of(state.getCurrentShufflingStartShard()
            .plus(current_committees_per_epoch).modulo(getConstants().getShardCount()));
      } else if (epochs_since_last_registry_update.greater(EpochNumber.of(1)) &&
          is_power_of_two(epochs_since_last_registry_update)) {
        /*
          elif epochs_since_last_registry_update > 1 and is_power_of_two(epochs_since_last_registry_update):
            committees_per_epoch = get_next_epoch_committee_count(state)
            seed = generate_seed(state, next_epoch)
            shuffling_epoch = next_epoch
            shuffling_start_shard = state.current_shuffling_start_shard */

        committees_per_epoch = get_next_epoch_committee_count(state);
        seed = generate_seed(state, nextEpoch);
        shuffling_epoch = nextEpoch;
        shuffling_start_shard = state.getCurrentShufflingStartShard();
      } else {
        /*
          else:
            committees_per_epoch = get_current_epoch_committee_count(state)
            seed = state.current_shuffling_seed
            shuffling_epoch = state.current_shuffling_epoch
            shuffling_start_shard = state.current_shuffling_start_shard */

        committees_per_epoch = get_current_epoch_committee_count(state);
        seed = state.getCurrentShufflingSeed();
        shuffling_epoch = state.getCurrentShufflingEpoch();
        shuffling_start_shard = state.getCurrentShufflingStartShard();
      }
    } else {
      throw new BeaconChainSpec.SpecAssertionFailed();
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
    SlotNumber offset = slot.modulo(getConstants().getSlotsPerEpoch());
    UInt64 committees_per_slot = UInt64.valueOf(committees_per_epoch).dividedBy(
        getConstants().getSlotsPerEpoch());
    ShardNumber slot_start_shard = ShardNumber.of(
        shuffling_start_shard.plus(committees_per_slot).times(offset).modulo(
            getConstants().getShardCount()));

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
          slot_start_shard.plusModulo(i, getConstants().getShardCount()));
      ret.add(committee);
    }

    return ret;
  }

  /*
    def get_beacon_proposer_index(state: BeaconState,
                              slot: Slot,
                              registry_change: bool=False) -> ValidatorIndex:
      """
      Return the beacon proposer index for the ``slot``.
      """
      epoch = slot_to_epoch(slot)
      current_epoch = get_current_epoch(state)
      previous_epoch = get_previous_epoch(state)
      next_epoch = current_epoch + 1

      assert previous_epoch <= epoch <= next_epoch

      first_committee, _ = get_crosslink_committees_at_slot(state, slot, registry_change)[0]
      return first_committee[epoch % len(first_committee)]
    */
  default ValidatorIndex get_beacon_proposer_index(BeaconState state, SlotNumber slot, boolean registryChange) {
    EpochNumber epoch = slot_to_epoch(slot);
    EpochNumber currentEpoch = get_current_epoch(state);
    EpochNumber previousEpoch = get_previous_epoch(state);
    EpochNumber nextEpoch = currentEpoch.increment();

    assertTrue(previousEpoch.lessEqual(epoch) && epoch.lessEqual(nextEpoch));

    List<ValidatorIndex> first_committee =
        get_crosslink_committees_at_slot(state, slot, registryChange).get(0).getCommittee();
    return first_committee.get(epoch.modulo(first_committee.size()).getIntValue());
  }

  default ValidatorIndex get_beacon_proposer_index(BeaconState state, SlotNumber slot) {
    return get_beacon_proposer_index(state, slot, false);
  }

  /*
    def is_active_validator(validator: Validator, epoch: EpochNumber) -> bool:
        """
        Check if ``validator`` is active.
        """
        return validator.activation_epoch <= epoch < validator.exit_epoch
    */
  default boolean is_active_validator(ValidatorRecord validator, EpochNumber epoch) {
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
  default List<ValidatorIndex>  get_active_validator_indices(
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
  default Hash32 get_randao_mix(BeaconState state, EpochNumber epoch) {
    assertTrue(get_current_epoch(state).minus(getConstants().getLatestRandaoMixesLength()).less(epoch));
    assertTrue(epoch.lessEqual(get_current_epoch(state)));
    return state.getLatestRandaoMixes().get(
        epoch.modulo(getConstants().getLatestRandaoMixesLength()));
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
  default UInt64 get_permuted_index(UInt64 index, UInt64 listSize, Bytes32 seed) {
    assertTrue(index.compareTo(listSize) < 0);
    assertTrue(listSize.compareTo(UInt64.valueOf(1L << 40)) <= 0);

    for (int round = 0; round < getConstants().getShuffleRoundCount(); round++) {
      Bytes8 pivotBytes = Bytes8.wrap(hash(seed.concat(int_to_bytes1(round))), 0);
      long pivot = bytes_to_int(pivotBytes).modulo(listSize).getValue();
      UInt64 flip = UInt64.valueOf(Math.floorMod(pivot - index.getValue(), listSize.getValue()));
      UInt64 position = UInt64s.max(index, flip);
      Bytes4 positionBytes = int_to_bytes4(position.dividedBy(UInt64.valueOf(256)));
      Bytes32 source = hash(seed.concat(int_to_bytes1(round)).concat(positionBytes));
      int byteV = source.get(position.modulo(256).getIntValue() / 8) & 0xFF;
      int bit = ((byteV >> (position.modulo(8).getIntValue())) % 2) & 0xFF;
      index = bit > 0 ? flip : index;
    }

    return index;
  }

  /**
   * An optimized version of list shuffling.
   *
   * Ported from https://github.com/protolambda/eth2-shuffle/blob/master/shuffle.go#L159
   * Note: the spec uses inverse direction of index mutations,
   *       hence round order is inverse
   */
  default List<UInt64> get_permuted_list(List<? extends UInt64> indices, Bytes32 seed) {
    if (indices.size() < 2) {
      return new ArrayList<>(indices);
    }

    int listSize = indices.size();
    List<UInt64> permutations = new ArrayList<>(indices);

    for (int round = getConstants().getShuffleRoundCount() - 1; round >= 0; round--) {
      BytesValue roundSeed = seed.concat(int_to_bytes1(round));
      Bytes8 pivotBytes = Bytes8.wrap(hash(roundSeed), 0);
      long pivot = bytes_to_int(pivotBytes).modulo(listSize).getValue();

      long mirror = (pivot + 1) >>> 1;
      Bytes32 source = hash(roundSeed.concat(int_to_bytes4(pivot >>> 8)));

      byte byteV = source.get((int) ((pivot & 0xff) >>> 3));
      for (long i = 0, j = pivot; i < mirror; ++i, --j) {
        if ((j & 0xff) == 0xff) {
          source = hash(roundSeed.concat(int_to_bytes4(j >>> 8)));
        }
        if ((j & 0x7) == 0x7) {
          byteV = source.get((int) ((j & 0xff) >>> 3));
        }

        byte bitV = (byte) ((byteV >>> (j & 0x7)) & 0x1);
        if (bitV == 1) {
          UInt64 oldV = permutations.get((int) i);
          permutations.set((int) i, permutations.get((int) j));
          permutations.set((int) j, oldV);
        }
      }

      mirror = (pivot + listSize + 1) >>> 1;
      long end = listSize - 1;

      source = hash(roundSeed.concat(int_to_bytes4(end >>> 8)));
      byteV = source.get((int) ((end & 0xff) >>> 3));
      for (long i = pivot + 1, j = end; i < mirror; ++i, --j) {
        if ((j & 0xff) == 0xff) {
          source = hash(roundSeed.concat(int_to_bytes4(j >>> 8)));
        }
        if ((j & 0x7) == 0x7) {
          byteV = source.get((int) ((j & 0xff) >>> 3));
        }

        byte bitV = (byte) ((byteV >>> (j & 0x7)) & 0x1);
        if (bitV == 1) {
          UInt64 oldV = permutations.get((int) i);
          permutations.set((int) i, permutations.get((int) j));
          permutations.set((int) j, oldV);
        }
      }
    }

    return permutations;
  }

  default UInt64 bytes_to_int(Bytes8 bytes) {
    return UInt64.fromBytesLittleEndian(bytes);
  }

  default UInt64 bytes_to_int(BytesValue bytes) {
    return bytes_to_int(Bytes8.wrap(bytes, 0));
  }

  default BytesValue int_to_bytes1(int value) {
    return BytesValues.ofUnsignedByte(value);
  }

  default Bytes4 int_to_bytes4(long value) {
    return Bytes4.ofUnsignedIntLittleEndian(value & 0xFFFFFF);
  }

  default Bytes4 int_to_bytes4(UInt64 value) {
    return int_to_bytes4(value.getValue());
  }

  default Bytes32 int_to_bytes32(UInt64 value) {
    return Bytes32.rightPad(value.toBytes8LittleEndian());
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
  default  <T> List<List<T>> split(List<T> values, int split_count) {
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
  default List<List<ValidatorIndex>> get_shuffling(Hash32 seed,
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
  default List<List<ValidatorIndex>> get_shuffling2(Hash32 seed,
      ReadList<ValidatorIndex, ValidatorRecord> validators,
      EpochNumber epoch) {
    List<ValidatorIndex> active_validator_indices = get_active_validator_indices(validators, epoch);
    int length = active_validator_indices.size();

    List<ValidatorIndex> shuffled_indices = get_permuted_list(active_validator_indices, seed)
        .stream().map(ValidatorIndex::new).collect(toList());

    return split(shuffled_indices, get_epoch_committee_count(length));
  }

  /*
   def verify_merkle_branch(leaf: Bytes32, proof: List[Bytes32], depth: int, index: int, root: Bytes32) -> bool:
    """
    Verify that the given ``leaf`` is on the merkle branch ``proof``
    starting with the given ``root``.
    """
    value = leaf
    for i in range(depth):
        if index // (2**i) % 2:
            value = hash(proof[i] + value)
        else:
            value = hash(value + proof[i])
    return value == root
  */
  default boolean verify_merkle_branch(
      Hash32 leaf, List<Hash32> proof, UInt64 depth, UInt64 index, Hash32 root) {
    Hash32 value = leaf;
    for (int i = 0; i < depth.getIntValue(); i++) {
      if (((index.getValue() >>> i) & 1) == 1) {
        value = hash(proof.get(i).concat(value));
      } else {
        value = hash(value.concat(proof.get(i)));
      }
    }

    return value.equals(root);
  }

  /*
   get_effective_balance(state: State, index: int) -> int:
     """
     Returns the effective balance (also known as "balance at stake") for a ``validator`` with the given ``index``.
     """
     return min(state.validator_balances[index], MAX_DEPOSIT * GWEI_PER_ETH)
  */
  default Gwei get_effective_balance(BeaconState state, ValidatorIndex validatorIdx) {
    return UInt64s.min(
        state.getValidatorBalances().get(validatorIdx),
        getConstants().getMaxDepositAmount());
  }

  /*
    def get_total_balance(state: BeaconState, validators: List[ValidatorIndex]) -> Gwei:
      """
      Return the combined effective balance of an array of ``validators``.
      """
      return sum([get_effective_balance(state, i) for i in validators])
   */
  default Gwei get_total_balance(BeaconState state, Collection<ValidatorIndex> validators) {
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
  default UInt64 integer_squareroot(UInt64 n) {
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
  default boolean is_power_of_two(UInt64 value) {
    return Long.bitCount(value.getValue()) == 1;
  }

  /*
    def process_deposit(state: BeaconState, deposit: Deposit) -> None:
      """
      Process a deposit from Ethereum 1.0.
      Note that this function mutates ``state``.
      """
    */
  default void process_deposit(
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

  default void process_deposit_inner(
      MutableBeaconState state,
      Deposit deposit,
      boolean verifyProof) {

    /* deposit_input = deposit.deposit_data.deposit_input */
    DepositInput deposit_input = deposit.getDepositData().getDepositInput();

    /*
      # Increment the next deposit index we are expecting. Note that this
      # needs to be done here because while the deposit contract will never
      # create an invalid Merkle branch, it may admit an invalid deposit
      # object, and we need to be able to skip over it
      state.deposit_index += 1
     */
    state.setDepositIndex(state.getDepositIndex().increment());

    /*
      validator_pubkeys = [v.pubkey for v in state.validator_registry]
      pubkey = deposit_input.pubkey
      amount = deposit.deposit_data.amount
      withdrawal_credentials = deposit_input.withdrawal_credentials
     */
    BLSPubkey pubkey = deposit_input.getPubKey();
    Gwei amount = deposit.getDepositData().getAmount();
    Hash32 withdrawal_credentials = deposit_input.getWithdrawalCredentials();
    ValidatorIndex index = get_validator_index_by_pubkey(state, pubkey);

    // if pubkey not in validator_pubkeys:
    if (index.equals(ValidatorIndex.MAX)) {
      /*
        # Verify the proof of possession
        proof_is_valid = bls_verify(
            pubkey=deposit_input.pubkey,
            message_hash=signed_root(deposit_input),
            signature=deposit_input.proof_of_possession,
            domain=get_domain(
                state.fork,
                get_current_epoch(state),
                DOMAIN_DEPOSIT,
            )
        )
        if not proof_is_valid:
            return */

      boolean proof_is_valid =
          !verifyProof
              || bls_verify(
              deposit_input.getPubKey(),
              signed_root(deposit_input),
              deposit_input.getProofOfPossession(),
              get_domain(state.getFork(), get_current_epoch(state), SignatureDomains.DEPOSIT));

      if (!proof_is_valid) {
        return;
      }

      // Add new validator
      ValidatorRecord validator = new ValidatorRecord(
          pubkey,
          withdrawal_credentials,
          getConstants().getFarFutureEpoch(),
          getConstants().getFarFutureEpoch(),
          getConstants().getFarFutureEpoch(),
          Boolean.FALSE,
          Boolean.FALSE);

      // Note: In phase 2 registry indices that have been withdrawn for a long time will be
      // recycled.
      state.getValidatorRegistry().add(validator);
      state.getValidatorBalances().add(amount);
    } else {
      // Increase balance by deposit amount
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
  default EpochNumber get_delayed_activation_exit_epoch(EpochNumber epoch) {
    return epoch.plus(1).plus(getConstants().getActivationExitDelay());
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
  default void activate_validator(MutableBeaconState state, ValidatorIndex index, boolean genesis) {
    EpochNumber activationSlot =
        genesis ? getConstants().getGenesisEpoch() :
            get_delayed_activation_exit_epoch(get_current_epoch(state));
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
  default void slash_validator(MutableBeaconState state, ValidatorIndex index) {
    ValidatorRecord validator = state.getValidatorRegistry().get(index);
    assertTrue(state.getSlot().less(get_epoch_start_slot(validator.getWithdrawableEpoch())));
    exit_validator(state, index);
    state.getLatestSlashedBalances().update(
        get_current_epoch(state).modulo(getConstants().getLatestSlashedExitLength()),
        balance -> balance.plus(get_effective_balance(state, index)));

    ValidatorIndex whistleblower_index = get_beacon_proposer_index(state, state.getSlot());
    Gwei whistleblower_reward = get_effective_balance(state, index)
        .dividedBy(getConstants().getWhistleblowerRewardQuotient());
    state.getValidatorBalances().update(whistleblower_index,
        oldVal -> oldVal.plus(whistleblower_reward));
    state.getValidatorBalances().update(index,
        oldVal -> oldVal.minus(whistleblower_reward));
    state.getValidatorRegistry().update(index,
        v -> v.builder().withSlashed(Boolean.TRUE)
            .withWithdrawableEpoch(get_current_epoch(state).plus(getConstants().getLatestSlashedExitLength()))
            .build());
  }

  /*
   def initiate_validator_exit(state: BeaconState, index: int) -> None:
     validator = state.validator_registry[index]
     validator.initiated_exit = True
  */
  default void initiate_validator_exit(MutableBeaconState state, ValidatorIndex index) {
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
      delayed_activation_exit_epoch = get_delayed_activation_exit_epoch(get_current_epoch(state))

      # The following updates only occur if not previous exited
      if validator.exit_epoch <= delayed_activation_exit_epoch:
          return
      else:
          validator.exit_epoch = delayed_activation_exit_epoch
   */
  default void exit_validator(MutableBeaconState state, ValidatorIndex index) {
    ValidatorRecord validator = state.getValidatorRegistry().get(index);
    EpochNumber delayed_activation_exit_epoch =
        get_delayed_activation_exit_epoch(get_current_epoch(state));

    // The following updates only occur if not previous exited
    if (validator.getExitEpoch().lessEqual(delayed_activation_exit_epoch)) {
      return;
    } else {
      state.getValidatorRegistry().update(index, v ->
          v.builder().withExitEpoch(delayed_activation_exit_epoch).build());
    }
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
  default void prepare_validator_for_withdrawal(MutableBeaconState state, ValidatorIndex index) {
    state
        .getValidatorRegistry()
        .update(
            index,
            v ->
                v.builder()
                    .withWithdrawableEpoch(
                        get_current_epoch(state).plus(getConstants().getMinValidatorWithdrawabilityDelay()))
                    .build());
  }

  /** Function for hashing objects into a single root utilizing a hash tree structure */
  default Hash32 hash_tree_root(Object object) {
    return getObjectHasher().getHash(object);
  }

  /** Function for hashing self-signed objects */
  default Hash32 signed_root(Object object) {
    return getObjectHasher().getHashTruncateLast(object);
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
  default Hash32 get_active_index_root(BeaconState state, EpochNumber epoch) {
    assertTrue(get_current_epoch(state).minus(getConstants().getLatestActiveIndexRootsLength()).plus(
        getConstants().getActivationExitDelay())
        .less(epoch));
    assertTrue(epoch.lessEqual(get_current_epoch(state).plus(getConstants().getActivationExitDelay())));
    return state.getLatestActiveIndexRoots().get(epoch.modulo(getConstants().getLatestActiveIndexRootsLength()));
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
  default Hash32 generate_seed(BeaconState state, EpochNumber epoch) {
    return hash(
        get_randao_mix(state, epoch.minus(getConstants().getMinSeedLookahead()))
            .concat(get_active_index_root(state, epoch))
            .concat(int_to_bytes32(epoch)));
  }

  default boolean bls_verify(BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    PublicKey blsPublicKey = PublicKey.create(publicKey);
    return bls_verify(blsPublicKey, message, signature, domain);
  }

  default boolean bls_verify(
      PublicKey blsPublicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    MessageParameters messageParameters = MessageParameters.create(message, domain);
    Signature blsSignature = Signature.create(signature);
    return BLS381.verify(messageParameters, blsSignature, blsPublicKey);
  }

  default boolean bls_verify_multiple(
      List<PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, UInt64 domain) {
    List<MessageParameters> messageParameters =
        messages.stream()
            .map(hash -> MessageParameters.create(hash, domain))
            .collect(Collectors.toList());
    Signature blsSignature = Signature.create(signature);
    return BLS381.verifyMultiple(messageParameters, blsSignature, publicKeys);
  }

  default PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
    List<PublicKey> publicKeys = publicKeysBytes.stream().map(PublicKey::create).collect(toList());
    return PublicKey.aggregate(publicKeys);
  }

  /*
    def get_fork_version(fork: Fork,
                     epoch: EpochNumber) -> bytes:
    """
    Return the fork version of the given ``epoch``.
    """
    if epoch < fork.epoch:
        return fork.previous_version
    else:
        return fork.current_version
   */
  default Bytes4 get_fork_version(Fork fork, EpochNumber epoch) {
    if (epoch.less(fork.getEpoch())) {
      return fork.getPreviousVersion();
    } else {
      return fork.getCurrentVersion();
    }
  }

  /*
    def get_domain(fork: Fork,
               epoch: Epoch,
               domain_type: int) -> int:
    """
    Get the domain number that represents the fork meta and signature domain.
    """
    return bytes_to_int(get_fork_version(fork, epoch) + int_to_bytes4(domain_type))
   */
  default UInt64 get_domain(Fork fork, EpochNumber epoch, UInt64 domainType) {
    return bytes_to_int(get_fork_version(fork, epoch).concat(int_to_bytes4(domainType)));
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
  default boolean is_double_vote(
      AttestationData attestation_data_1, AttestationData attestation_data_2) {
    EpochNumber target_epoch_1 = slot_to_epoch(attestation_data_1.getSlot());
    EpochNumber target_epoch_2 = slot_to_epoch(attestation_data_2.getSlot());
    return target_epoch_1.equals(target_epoch_2);
  }

  /*
   def is_surround_vote(attestation_data_1: AttestationData,
                     attestation_data_2: AttestationData) -> bool:
      """
      Check if ``attestation_data_1`` surrounds ``attestation_data_2``.
      """
      source_epoch_1 = attestation_data_1.source_epoch
      source_epoch_2 = attestation_data_2.source_epoch
      target_epoch_1 = slot_to_epoch(attestation_data_1.slot)
      target_epoch_2 = slot_to_epoch(attestation_data_2.slot)

      return source_epoch_1 < source_epoch_2 and target_epoch_2 < target_epoch_1
  */
  default boolean is_surround_vote(
      AttestationData attestation_data_1, AttestationData attestation_data_2) {
    EpochNumber source_epoch_1 = attestation_data_1.getSourceEpoch();
    EpochNumber source_epoch_2 = attestation_data_2.getSourceEpoch();
    EpochNumber target_epoch_1 = slot_to_epoch(attestation_data_1.getSlot());
    EpochNumber target_epoch_2 = slot_to_epoch(attestation_data_2.getSlot());

    return (source_epoch_1.less(source_epoch_2))
        && (target_epoch_2.less(target_epoch_1));
  }

  default List<BLSPubkey> mapIndicesToPubKeys(BeaconState state, Iterable<ValidatorIndex> indices) {
    List<BLSPubkey> publicKeys = new ArrayList<>();
    for (ValidatorIndex index : indices) {
      checkIndexRange(state, index);
      publicKeys.add(state.getValidatorRegistry().get(index).getPubKey());
    }
    return publicKeys;
  }

  /*
    def verify_slashable_attestation(state: BeaconState, slashable_attestation: SlashableAttestation) -> bool:
      """
      Verify validity of ``slashable_attestation`` fields.
      """
   */
  default boolean verify_slashable_attestation(BeaconState state, SlashableAttestation slashable_attestation) {
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
        compareTo(getConstants().getMaxIndicesPerSlashableVote()) > 0) {
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
        slashable_attestation.getAggregateSingature(),
        get_domain(state.getFork(), slot_to_epoch(slashable_attestation.getData().getSlot()), ATTESTATION));
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
  default boolean verify_bitfield(Bitfield bitfield, int committee_size) {
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
                   slot: Slot) -> Bytes32:
    """
    Return the block root at a recent ``slot``.
    """
    assert slot < state.slot <= slot + SLOTS_PER_HISTORICAL_ROOT
    return state.latest_block_roots[slot % SLOTS_PER_HISTORICAL_ROOT]
  */
  default Hash32 get_block_root(BeaconState state, SlotNumber slot) {
    assertTrue(state.getSlot().lessEqual(slot.plus(getConstants().getSlotsPerHistoricalRoot())));
    assertTrue(slot.less(state.getSlot()));
    return state.getLatestBlockRoots().get(slot.modulo(getConstants().getSlotsPerHistoricalRoot()));
  }

  /*
    def get_state_root(state: BeaconState,
                   slot: Slot) -> Bytes32:
      """
      Return the state root at a recent ``slot``.
      """
      assert slot < state.slot <= slot + SLOTS_PER_HISTORICAL_ROOT
      return state.latest_state_roots[slot % SLOTS_PER_HISTORICAL_ROOT]
   */
  default Hash32 get_state_root(BeaconState state, SlotNumber slot) {
    assertTrue(state.getSlot().lessEqual(slot.plus(getConstants().getSlotsPerHistoricalRoot())));
    assertTrue(slot.less(state.getSlot()));
    return state.getLatestStateRoots().get(slot.modulo(getConstants().getSlotsPerHistoricalRoot()));
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
  default List<ValidatorIndex> get_attestation_participants(
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

  default ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    ValidatorIndex index = ValidatorIndex.MAX;
    for (ValidatorIndex i : state.getValidatorRegistry().size()) {
      if (state.getValidatorRegistry().get(i).getPubKey().equals(pubkey)) {
        index = i;
        break;
      }
    }

    return index;
  }

  /*
   def slot_to_epoch(slot: SlotNumber) -> EpochNumber:
       return slot // SLOTS_PER_EPOCH
  */
  default EpochNumber slot_to_epoch(SlotNumber slot) {
    return slot.dividedBy(getConstants().getSlotsPerEpoch());
  }

  /*
    def get_previous_epoch(state: BeaconState) -> Epoch:
      """`
      Return the previous epoch of the given ``state``.
      """
      return get_current_epoch(state) - 1
   */
  default EpochNumber get_previous_epoch(BeaconState state) {
    return get_current_epoch(state).decrement();
  }

  /*
   def get_current_epoch(state: BeaconState) -> EpochNumber:
       return slot_to_epoch(state.slot)
  */
  default EpochNumber get_current_epoch(BeaconState state) {
    return slot_to_epoch(state.getSlot());
  }
  /*
   def get_epoch_start_slot(epoch: EpochNumber) -> SlotNumber:
     return epoch * SLOTS_PER_EPOCH
  */
  default SlotNumber get_epoch_start_slot(EpochNumber epoch) {
    return epoch.mul(getConstants().getSlotsPerEpoch());
  }
}

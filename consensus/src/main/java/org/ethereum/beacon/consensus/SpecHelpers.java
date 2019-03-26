package org.ethereum.beacon.consensus;

import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
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
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.SlashableAttestation;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.HistoricalBatch;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.Bitfield64;
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
 * https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#helper-functions
 */
public interface SpecHelpers {

  /**
   * Creates a SpecHelpers instance with given {@link SpecConstants} and time supplier,
   * {@link Hashes#keccak256(BytesValue)} as a hash function and {@link SSZObjectHasher} as an object
   * hasher.
   *
   * @param constants a chain getConstants().
   *    <code>Schedulers::currentTime</code> is passed
   * @return spec helpers instance.
   */
  static SpecHelpers createWithSSZHasher(@Nonnull SpecConstants constants) {
    Objects.requireNonNull(constants);

    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
    ObjectHasher<Hash32> sszHasher = SSZObjectHasher.create(hashFunction);
    return new SpecHelpersImpl(constants, hashFunction, sszHasher);
  }

  SpecConstants getConstants();

  ObjectHasher<Hash32> getObjectHasher();

  Function<BytesValue, Hash32> getHashFunction();

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
   */
  default List<UInt64> get_permuted_list(List<? extends UInt64> indices, Bytes32 seed) {
    if (indices.size() < 2) {
      return new ArrayList<>(indices);
    }

    int listSize = indices.size();
    List<UInt64> permutations = new ArrayList<>(indices);

    for (int round = 0; round < getConstants().getShuffleRoundCount(); round++) {
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

  default BytesValue int_to_bytes32(UInt64 value) {
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

  default SlotNumber get_current_slot(BeaconState state, long systemTime) {
    Millis currentTime = Millis.of(systemTime);
    assertTrue(state.getGenesisTime().lessEqual(currentTime.getSeconds()));
    Time sinceGenesis = currentTime.getSeconds().minus(state.getGenesisTime());
    return SlotNumber.castFrom(sinceGenesis.dividedBy(getConstants().getSecondsPerSlot()))
        .plus(getConstants().getGenesisSlot());
  }

  default boolean is_current_slot(BeaconState state, long systemTime) {
    return state.getSlot().equals(get_current_slot(state, systemTime));
  }

  default Time get_slot_start_time(BeaconState state, SlotNumber slot) {
    return state
        .getGenesisTime()
        .plus(getConstants().getSecondsPerSlot().times(slot.minus(getConstants().getGenesisSlot())));
  }

  default Time get_slot_middle_time(BeaconState state, SlotNumber slot) {
    return get_slot_start_time(state, slot).plus(getConstants().getSecondsPerSlot().dividedBy(2));
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

  default void checkIndexRange(BeaconState state, ValidatorIndex index) {
    assertTrue(index.less(state.getValidatorRegistry().size()));
  }

  default void checkIndexRange(BeaconState state, Iterable<ValidatorIndex> indices) {
    indices.forEach(index -> checkIndexRange(state, index));
  }

  default void checkShardRange(ShardNumber shard) {
    assertTrue(shard.less(getConstants().getShardCount()));
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
  default BeaconBlock lmd_ghost(
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
      List<BeaconBlock> children = getChildrenBlocks.apply(signed_root(head));
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
  default Optional<BeaconBlock> get_latest_attestation_target(
      ValidatorRecord validatorRecord,
      Function<ValidatorRecord, Optional<Attestation>> get_latest_attestation,
      Function<Hash32, Optional<BeaconBlock>> getBlock) {
    Optional<Attestation> latest = get_latest_attestation.apply(validatorRecord);
    return latest.flatMap(at -> getBlock.apply(at.getData().getSourceRoot()));
  }

  /*
    def get_vote_count(block: BeaconBlock) -> int:
      return sum(
          get_effective_balance(start_state.validator_balances[validator_index]) // FORK_CHOICE_BALANCE_INCREMENT
          for validator_index, target in attestation_targets
          if get_ancestor(store, target, block.slot) == block
      )
   */
  default UInt64 get_vote_count(
      BeaconState startState,
      BeaconBlock block,
      List<Pair<ValidatorIndex, BeaconBlock>> attestation_targets,
      Function<Hash32, Optional<BeaconBlock>> getBlock) {

    return attestation_targets.stream().filter(
        target -> get_ancestor(target.getValue1(), block.getSlot(), getBlock)
            .filter(ancestor -> ancestor.equals(block)).isPresent())
        .map(target -> get_effective_balance(startState, target.getValue0()).dividedBy(getConstants().getForkChoiceBalanceIncrement()))
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
  default Optional<BeaconBlock> get_ancestor(
      BeaconBlock block, SlotNumber slot, Function<Hash32, Optional<BeaconBlock>> getBlock) {
    if (block.getSlot().equals(slot)) {
      return Optional.of(block);
    } else if (block.getSlot().less(slot)) {
      return Optional.empty();
    } else {
      return getBlock
          .apply(block.getPreviousBlockRoot())
          .flatMap(parent -> get_ancestor(parent, slot, getBlock));
    }
  }

  default boolean is_epoch_end(SlotNumber slot) {
    return slot.increment().modulo(getConstants().getSlotsPerEpoch()).equals(SlotNumber.ZERO);
  }

  /*
   """
   Get an empty ``BeaconBlock``.
   """
  */
  default BeaconBlock get_empty_block() {
    BeaconBlockBody body =
        new BeaconBlockBody(
            getConstants().getEmptySignature(),
            new Eth1Data(Hash32.ZERO, Hash32.ZERO),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList());
    return new BeaconBlock(
        getConstants().getGenesisSlot(), Hash32.ZERO, Hash32.ZERO, body, getConstants().getEmptySignature());
  }

  /*
   """
   Get the genesis ``BeaconState``.
   """
  */
  default BeaconState get_genesis_beacon_state(
      List<Deposit> genesisValidatorDeposits, Time genesisTime, Eth1Data genesisEth1Data) {
    MutableBeaconState state = BeaconState.getEmpty().createMutableCopy();

    // Misc
    state.setSlot(getConstants().getGenesisSlot());
    state.setGenesisTime(genesisTime);
    state.setFork(new Fork(
        int_to_bytes4(getConstants().getGenesisForkVersion()),
        int_to_bytes4(getConstants().getGenesisForkVersion()),
        getConstants().getGenesisEpoch()));

    // Validator registry
    state.getValidatorRegistry().clear();
    state.getValidatorBalances().clear();
    state.setValidatorRegistryUpdateEpoch(getConstants().getGenesisEpoch());

    // Randomness and committees
    state.getLatestRandaoMixes().addAll(
        nCopies(getConstants().getLatestRandaoMixesLength().getIntValue(), Hash32.ZERO));
    state.setPreviousShufflingStartShard(getConstants().getGenesisStartShard());
    state.setCurrentShufflingStartShard(getConstants().getGenesisStartShard());
    state.setPreviousShufflingEpoch(getConstants().getGenesisEpoch());
    state.setCurrentShufflingEpoch(getConstants().getGenesisEpoch());
    state.setPreviousShufflingSeed(Hash32.ZERO);
    state.setCurrentShufflingSeed(Hash32.ZERO);

    // Finality
    state.getPreviousEpochAttestations().clear();
    state.getCurrentEpochAttestations().clear();
    state.setPreviousJustifiedEpoch(getConstants().getGenesisEpoch());
    state.setCurrentJustifiedEpoch(getConstants().getGenesisEpoch());
    state.setPreviousJustifiedRoot(Hash32.ZERO);
    state.setCurrentJustifiedRoot(Hash32.ZERO);
    state.setJustificationBitfield(Bitfield64.ZERO);
    state.setFinalizedEpoch(getConstants().getGenesisEpoch());
    state.setFinalizedRoot(Hash32.ZERO);

    // Recent state
    state.getLatestCrosslinks().addAll(
        nCopies(getConstants().getShardCount().getIntValue(),
            new Crosslink(getConstants().getGenesisEpoch(), Hash32.ZERO)));
    state.getLatestBlockRoots().addAll(
        nCopies(getConstants().getSlotsPerHistoricalRoot().getIntValue(), Hash32.ZERO));
    state.getLatestStateRoots().addAll(
        nCopies(getConstants().getSlotsPerHistoricalRoot().getIntValue(), Hash32.ZERO));
    state.getLatestActiveIndexRoots().addAll(
        nCopies(getConstants().getLatestActiveIndexRootsLength().getIntValue(), Hash32.ZERO));
    state.getLatestSlashedBalances().addAll(
        nCopies(getConstants().getLatestSlashedExitLength().getIntValue(), Gwei.ZERO));
    state.setLatestBlockHeader(get_temporary_block_header(get_empty_block()));
    state.getHistoricalRoots().clear();

    // Ethereum 1.0 chain data
    state.setLatestEth1Data(genesisEth1Data);
    state.getEth1DataVotes().clear();
    state.setDepositIndex(UInt64.ZERO);

    // Process genesis deposits
    for (Deposit deposit : genesisValidatorDeposits) {
      process_deposit(state, deposit);
    }

    // Process genesis activations
    for (ValidatorIndex validatorIndex : state.getValidatorRegistry().size().iterateFromZero()) {
      if (get_effective_balance(state, validatorIndex).greaterEqual(getConstants().getMaxDepositAmount())) {
        activate_validator(state, validatorIndex, true);
      }
    }

    Hash32 genesisActiveIndexRoot = hash_tree_root(
        get_active_validator_indices(state.getValidatorRegistry(), getConstants().getGenesisEpoch()));

    for (EpochNumber index : getConstants().getLatestActiveIndexRootsLength().iterateFrom(EpochNumber.ZERO)) {
      state.getLatestActiveIndexRoots().set(index, genesisActiveIndexRoot);
    }
    state.setCurrentShufflingSeed(generate_seed(state, getConstants().getGenesisEpoch()));

    return state.createImmutable();
  }

  /*
    At every slot > GENESIS_SLOT run the following function
    Note: this function mutates beacon state
   */
  default void cache_state(MutableBeaconState state) {
    Hash32 previousSlotStateRoot = hash_tree_root(state);

    // store the previous slot's post state transition root
    state.getLatestStateRoots()
        .set(state.getSlot().modulo(getConstants().getSlotsPerHistoricalRoot()), previousSlotStateRoot);

    // cache state root in stored latest_block_header if empty
    if (state.getLatestBlockHeader().getStateRoot().equals(Hash32.ZERO)) {
      state.setLatestBlockHeader(state.getLatestBlockHeader().withStateRoot(previousSlotStateRoot));
    }

    // store latest known block for previous slot
    state.getLatestBlockRoots()
        .set(
            state.getSlot().modulo(getConstants().getSlotsPerHistoricalRoot()),
            signed_root(state.getLatestBlockHeader()));
  }

  /*
    At every slot > GENESIS_SLOT run the following function:
    Note: this function mutates beacon state
   */
  default void advance_slot(MutableBeaconState state) {
    state.setSlot(state.getSlot().increment());
  }

  /*
    def get_current_total_balance(state: BeaconState) -> Gwei:
      return get_total_balance(state, get_active_validator_indices(state.validator_registry, get_current_epoch(state)))
   */
  default Gwei get_current_total_balance(BeaconState state) {
    return get_total_balance(state,
        get_active_validator_indices(state.getValidatorRegistry(), get_current_epoch(state)));
  }

  /*
    def get_previous_total_balance(state: BeaconState) -> Gwei:
      return get_total_balance(state, get_active_validator_indices(state.validator_registry, get_previous_epoch(state)))
   */
  default Gwei get_previous_total_balance(BeaconState state) {
    return get_total_balance(state,
        get_active_validator_indices(state.getValidatorRegistry(), get_previous_epoch(state)));
  }

  /*
    def get_attesting_indices(state: BeaconState, attestations: List[PendingAttestation]) -> List[ValidatorIndex]:
      output = set()
      for a in attestations:
          output = output.union(get_attestation_participants(state, a.data, a.aggregation_bitfield))
      return sorted(list(output))
   */
  default List<ValidatorIndex> get_attesting_indices(BeaconState state, List<PendingAttestation> attestations) {
    List<ValidatorIndex> output = new ArrayList<>();
    for (PendingAttestation a : attestations) {
      output.addAll(get_attestation_participants(state, a.getData(), a.getAggregationBitfield()));
    }
    Collections.sort(output);
    return output;
  }

  /*
    def get_attesting_balance(state: BeaconState, attestations: List[PendingAttestation]) -> Gwei:
      return get_total_balance(state, get_attesting_indices(state, attestations))
   */
  default Gwei get_attesting_balance(BeaconState state, List<PendingAttestation> attestations) {
    return get_total_balance(state, get_attesting_indices(state, attestations));
  }

  /*
    def get_current_epoch_boundary_attestations(state: BeaconState) -> List[PendingAttestation]:
      return [
          a for a in state.current_epoch_attestations
          if a.data.target_root == get_block_root(state, get_epoch_start_slot(get_current_epoch(state)))
      ]
   */
  default List<PendingAttestation> get_current_epoch_boundary_attestations(BeaconState state) {
    return state.getCurrentEpochAttestations().stream()
        .filter(a -> a.getData()
            .getTargetRoot()
            .equals(get_block_root(state, get_epoch_start_slot(get_current_epoch(state)))))
        .collect(toList());
  }

  /*
    def get_previous_epoch_boundary_attestations(state: BeaconState) -> List[PendingAttestation]:
      return [
          a for a in state.previous_epoch_attestations
          if a.data.target_root == get_block_root(state, get_epoch_start_slot(get_previous_epoch(state)))
      ]
   */
  default List<PendingAttestation> get_previous_epoch_boundary_attestations(BeaconState state) {
    return state.getCurrentEpochAttestations().stream()
        .filter(a -> a.getData()
            .getTargetRoot()
            .equals(get_block_root(state, get_epoch_start_slot(get_previous_epoch(state)))))
        .collect(toList());
  }

  /*
    def get_previous_epoch_matching_head_attestations(state: BeaconState) -> List[PendingAttestation]:
      return [
          a for a in state.previous_epoch_attestations
          if a.data.beacon_block_root == get_block_root(state, a.data.slot)
      ]
   */
  default List<PendingAttestation> get_previous_epoch_matching_head_attestations(BeaconState state) {
    return state.getCurrentEpochAttestations().stream()
        .filter(a -> a.getData()
            .getBeaconBlockRoot()
            .equals(get_block_root(state, a.getData().getSlot())))
        .collect(toList());
  }

  /*
    def get_attestations_for(root) -> List[PendingAttestation]:
        return [a for a in valid_attestations if a.data.crosslink_data_root == root]
   */
  default Pair<Hash32, List<ValidatorIndex>> get_winning_root_and_participants(BeaconState state, ShardNumber shard) {
    /*
      all_attestations = state.current_epoch_attestations + state.previous_epoch_attestations
      valid_attestations = [
          a for a in all_attestations if a.data.previous_crosslink == state.latest_crosslinks[shard]
      ]
      all_roots = [a.data.crosslink_data_root for a in valid_attestations] */

    List<PendingAttestation> all_attestations = state.getCurrentEpochAttestations().listCopy();
    all_attestations.addAll(state.getPreviousEpochAttestations().listCopy());
    List<PendingAttestation> valid_attestations =
        all_attestations.stream()
            .filter(a -> a.getData()
                .getPreviousCrosslink()
                .equals(state.getLatestCrosslinks().get(shard)))
            .collect(toList());
    List<Hash32> all_roots =
        valid_attestations.stream().map(a -> a.getData().getCrosslinkDataRoot()).collect(toList());

    // handle when no attestations for shard available
    if (all_roots.isEmpty())
      return Pair.with(Hash32.ZERO, emptyList());

    /*
      def get_attestations_for(root) -> List[PendingAttestation]:
        return [a for a in valid_attestations if a.data.crosslink_data_root == root]
     */

    // Winning crosslink root is the root with the most votes for it, ties broken in favor of
    // lexicographically higher hash
    // winning_root = max(all_roots, key=lambda r: (get_attesting_balance(state, get_attestations_for(r)), r))
    Hash32 winning_root = all_roots.stream().max((r1, r2) -> {
      Gwei balance_r1 = get_attesting_balance(state, valid_attestations.stream()
          .filter(a -> a.getData().getCrosslinkDataRoot().equals(r1))
          .collect(toList()));

      Gwei balance_r2 = get_attesting_balance(state, valid_attestations.stream()
          .filter(a -> a.getData().getCrosslinkDataRoot().equals(r1))
          .collect(toList()));

      if (balance_r1.equals(balance_r2)) {
        return r1.toString().compareTo(r2.toString());
      } else {
        return balance_r1.compareTo(balance_r2);
      }
    }).get();

    /*
      return winning_root, get_attesting_indices(state, get_attestations_for(winning_root))
    */
    return Pair.with(
        winning_root,
        get_attesting_indices(state, valid_attestations.stream()
            .filter(a -> a.getData().getCrosslinkDataRoot().equals(winning_root))
            .collect(toList())));
  }

  /*
    def earliest_attestation(state: BeaconState, validator_index: ValidatorIndex) -> PendingAttestation:
      return min([
          a for a in state.previous_epoch_attestations if
          validator_index in get_attestation_participants(state, a.data, a.aggregation_bitfield)
      ], key=lambda a: a.inclusion_slot)
   */
  default PendingAttestation earliest_attestation(BeaconState state, ValidatorIndex validatorIndex) {
    return state.getPreviousEpochAttestations().stream()
        .filter(a -> get_attestation_participants(state, a.getData(), a.getAggregationBitfield())
            .contains(validatorIndex))
        .min(Comparator.comparing(PendingAttestation::getInclusionSlot))
        .get();
  }

  /*
    def inclusion_slot(state: BeaconState, validator_index: ValidatorIndex) -> Slot:
      return earliest_attestation(state, validator_index).inclusion_slot
   */
  default SlotNumber inclusion_slot(BeaconState state, ValidatorIndex validatorIndex) {
    return earliest_attestation(state, validatorIndex).getInclusionSlot();
  }

  /*
    def inclusion_distance(state: BeaconState, validator_index: ValidatorIndex) -> int:
      attestation = earliest_attestation(state, validator_index)
      return attestation.inclusion_slot - attestation.data.slot
   */
  default SlotNumber inclusion_distance(BeaconState state, ValidatorIndex validatorIndex) {
    PendingAttestation attestation = earliest_attestation(state, validatorIndex);
    return attestation.getInclusionSlot().minus(attestation.getData().getSlot());
  }

  /*
    Note: this function mutates beacon state
   */
  default void update_justification_and_finalization(MutableBeaconState state) {
    /*
      new_justified_epoch = state.current_justified_epoch
      new_finalized_epoch = state.finalized_epoch
     */
    EpochNumber new_justified_epoch = state.getCurrentJustifiedEpoch();
    EpochNumber new_finalized_epoch = state.getFinalizedEpoch();

    // Rotate the justification bitfield up one epoch to make room for the current epoch
    state.setJustificationBitfield(state.getJustificationBitfield().shl(1));

    /*
      # If the previous epoch gets justified, fill the second last bit

      previous_boundary_attesting_balance = get_attesting_balance(state, get_previous_epoch_boundary_attestations(state))
      if previous_boundary_attesting_balance * 3 >= get_previous_total_balance(state) * 2:
        new_justified_epoch = get_current_epoch(state) - 1
        state.justification_bitfield |= 2
     */
    Gwei previous_boundary_attesting_balance = get_attesting_balance(state,
        get_previous_epoch_boundary_attestations(state));
    if (previous_boundary_attesting_balance.times(3)
        .greaterEqual(get_previous_total_balance(state).times(2))) {
      new_justified_epoch = get_current_epoch(state).decrement();
      state.setJustificationBitfield(state.getJustificationBitfield().or(2));
    }

    /*
      # If the current epoch gets justified, fill the last bit

      current_boundary_attesting_balance = get_attesting_balance(state, get_current_epoch_boundary_attestations(state))
      if current_boundary_attesting_balance * 3 >= get_current_total_balance(state) * 2:
        new_justified_epoch = get_current_epoch(state)
        state.justification_bitfield |= 1
     */
    Gwei current_boundary_attesting_balance =
        get_attesting_balance(state, get_current_epoch_boundary_attestations(state));
    if (current_boundary_attesting_balance.times(3).greaterEqual(get_current_total_balance(state).times(2))) {
      new_justified_epoch = get_current_epoch(state);
      state.setJustificationBitfield(state.getJustificationBitfield().or(1));
    }

    // Process finalizations

    /*
      bitfield = state.justification_bitfield
      current_epoch = get_current_epoch(state)
     */
    Bitfield64 bitfield = state.getJustificationBitfield();
    EpochNumber current_epoch = get_current_epoch(state);

    /*
      # The 2nd/3rd/4th most recent epochs are all justified, the 2nd using the 4th as source
      if (bitfield >> 1) % 8 == 0b111 and state.previous_justified_epoch == current_epoch - 3:
        new_finalized_epoch = state.previous_justified_epoch */
    if (((bitfield.getValue() >>> 1) % 8 == 0b111L)
        && (state.getPreviousJustifiedEpoch().equals(current_epoch.minus(3)))) {
      new_finalized_epoch = state.getPreviousJustifiedEpoch();
    }

    /*
      # The 2nd/3rd most recent epochs are both justified, the 2nd using the 3rd as source
      if (bitfield >> 1) % 4 == 0b11 and state.previous_justified_epoch == current_epoch - 2:
        new_finalized_epoch = state.previous_justified_epoch */
    if (((bitfield.getValue() >>> 1) % 4 == 0b11L)
        && (state.getPreviousJustifiedEpoch().equals(current_epoch.minus(2)))) {
      new_finalized_epoch = state.getPreviousJustifiedEpoch();
    }

    /*
      # The 1st/2nd/3rd most recent epochs are all justified, the 1st using the 3rd as source
      if (bitfield >> 0) % 8 == 0b111 and state.current_justified_epoch == current_epoch - 2:
          new_finalized_epoch = state.current_justified_epoch */
    if (((bitfield.getValue() >>> 0) % 8 == 0b111L)
        && (state.getCurrentJustifiedEpoch().equals(current_epoch.minus(2)))) {
      new_finalized_epoch = state.getCurrentJustifiedEpoch();
    }

    /*
      # The 1st/2nd most recent epochs are both justified, the 1st using the 2nd as source
      if (bitfield >> 0) % 4 == 0b11 and state.current_justified_epoch == current_epoch - 1:
          new_finalized_epoch = state.current_justified_epoch */
    if (((bitfield.getValue() >>> 0) % 4 == 0b11L)
        && (state.getCurrentJustifiedEpoch().equals(current_epoch.minus(1)))) {
      new_finalized_epoch = state.getCurrentJustifiedEpoch();
    }

    // Update state jusification/finality fields

    /*
      state.previous_justified_epoch = state.current_justified_epoch
      state.previous_justified_root = state.current_justified_root */
    state.setPreviousJustifiedEpoch(state.getCurrentJustifiedEpoch());
    state.setPreviousJustifiedRoot(state.getCurrentJustifiedRoot());

    /*
      if new_justified_epoch != state.current_justified_epoch:
        state.current_justified_epoch = new_justified_epoch
        state.current_justified_root = get_block_root(state, get_epoch_start_slot(new_justified_epoch)) */
    if (!new_justified_epoch.equals(state.getCurrentJustifiedEpoch())) {
      state.setCurrentJustifiedEpoch(new_justified_epoch);
      state.setCurrentJustifiedRoot(get_block_root(state, get_epoch_start_slot(new_justified_epoch)));
    }

    /*
      if new_finalized_epoch != state.finalized_epoch:
        state.finalized_epoch = new_finalized_epoch
        state.finalized_root = get_block_root(state, get_epoch_start_slot(new_finalized_epoch)) */
    if (!new_finalized_epoch.equals(state.getFinalizedEpoch())) {
      state.setFinalizedEpoch(new_finalized_epoch);
      state.setFinalizedRoot(get_block_root(state, get_epoch_start_slot(new_finalized_epoch)));
    }
  }

  /*
    Note: this function mutates beacon state

    def process_crosslinks(state: BeaconState) -> None:
      current_epoch = get_current_epoch(state)
      previous_epoch = current_epoch - 1
      next_epoch = current_epoch + 1
      for slot in range(get_epoch_start_slot(previous_epoch), get_epoch_start_slot(next_epoch)):
          for crosslink_committee, shard in get_crosslink_committees_at_slot(state, slot):
              winning_root, participants = get_winning_root_and_participants(state, shard)
              participating_balance = get_total_balance(state, participants)
              total_balance = get_total_balance(state, crosslink_committee)
              if 3 * participating_balance >= 2 * total_balance:
                  state.latest_crosslinks[shard] = Crosslink(
                      epoch=slot_to_epoch(slot),
                      crosslink_data_root=winning_root
                  )
   */
  default void process_crosslinks(MutableBeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    EpochNumber previous_epoch = current_epoch.decrement();
    EpochNumber next_epoch = current_epoch.increment();

    for (SlotNumber slot : get_epoch_start_slot(previous_epoch)
        .iterateTo(get_epoch_start_slot(next_epoch))) {

      List<ShardCommittee> committees_at_slot = get_crosslink_committees_at_slot(state, slot);
      for (ShardCommittee shard_and_committee : committees_at_slot) {
        Pair<Hash32, List<ValidatorIndex>> root_and_participants =
            get_winning_root_and_participants(state, shard_and_committee.getShard());
        Gwei participating_balance = get_total_balance(state, root_and_participants.getValue1());
        Gwei total_balance = get_total_balance(state, shard_and_committee.getCommittee());

        if (participating_balance.times(3).greaterEqual(total_balance.times(2))) {
          state.getLatestCrosslinks().set(shard_and_committee.getShard(), new Crosslink(
              slot_to_epoch(slot),
              root_and_participants.getValue0()
          ));
        }
      }
    }
  }

  /*
    Note: this function mutates beacon state

    def maybe_reset_eth1_period(state: BeaconState) -> None:
      if (get_current_epoch(state) + 1) % EPOCHS_PER_ETH1_VOTING_PERIOD == 0:
          for eth1_data_vote in state.eth1_data_votes:
              # If a majority of all votes were for a particular eth1_data value,
              # then set that as the new canonical value
              if eth1_data_vote.vote_count * 2 > EPOCHS_PER_ETH1_VOTING_PERIOD * SLOTS_PER_EPOCH:
                  state.latest_eth1_data = eth1_data_vote.eth1_data
          state.eth1_data_votes = []
  */
  default void maybe_reset_eth1_period(MutableBeaconState state) {
    if (get_current_epoch(state).increment().modulo(getConstants().getEpochsPerEth1VotingPeriod())
        .equals(EpochNumber.ZERO)) {
      for (Eth1DataVote eth1_data_vote : state.getEth1DataVotes()) {
        // If a majority of all votes were for a particular eth1_data value,
        // then set that as the new canonical value
        if (eth1_data_vote.getVoteCount().times(2)
            .compareTo(getConstants().getEpochsPerEth1VotingPeriod().times(getConstants().getSlotsPerEpoch())) > 0) {
          state.setLatestEth1Data(eth1_data_vote.getEth1Data());
        }
        state.getEth1DataVotes().clear();
      }
    }
  }

  /*
    def get_base_reward(state: BeaconState, index: ValidatorIndex) -> Gwei:
      if get_previous_total_balance(state) == 0:
          return 0

      adjusted_quotient = integer_squareroot(get_previous_total_balance(state)) // BASE_REWARD_QUOTIENT
      return get_effective_balance(state, index) // adjusted_quotient // 5
   */
  default Gwei get_base_reward(BeaconState state, ValidatorIndex index) {
    if (get_previous_total_balance(state).equals(Gwei.ZERO)) {
      return Gwei.ZERO;
    }

    UInt64 adjusted_quotient = integer_squareroot(
        get_previous_total_balance(state)).dividedBy(getConstants().getBaseRewardQuotient());
    return get_effective_balance(state, index).dividedBy(adjusted_quotient).dividedBy(5);
  }

  /*
    def get_inactivity_penalty(state: BeaconState, index: ValidatorIndex, epochs_since_finality: int) -> Gwei:
      return (
          get_base_reward(state, index) +
          get_effective_balance(state, index) * epochs_since_finality // INACTIVITY_PENALTY_QUOTIENT // 2
      )
   */
  default Gwei get_inactivity_penalty(BeaconState state, ValidatorIndex index, EpochNumber epochsSinceFinality) {
    return get_base_reward(state, index).plus(
        get_effective_balance(state, index)
            .times(epochsSinceFinality).dividedBy(getConstants().getInactivityPenaltyQuotient()).dividedBy(2)
    );
  }

  /*
    When blocks are finalizing normally...

    # deltas[0] for rewards
    # deltas[1] for penalties
   */
  default Gwei[][] compute_normal_justification_and_finalization_deltas(BeaconState state) {
    /*
      deltas = [
        [0 for index in range(len(state.validator_registry))],
        [0 for index in range(len(state.validator_registry))]
      ] */
    Gwei[][] deltas = {
        new Gwei[state.getValidatorRegistry().size().getIntValue()],
        new Gwei[state.getValidatorRegistry().size().getIntValue()]
    };
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      deltas[0][index.getIntValue()] = deltas[1][index.getIntValue()] = Gwei.ZERO;
    }

    // Some helper variables
    List<PendingAttestation> previous_epoch_attestations =
        state.getPreviousEpochAttestations().listCopy();
    List<PendingAttestation> boundary_attestations = get_previous_epoch_boundary_attestations(state);
    Gwei boundary_attesting_balance = get_attesting_balance(state, boundary_attestations);
    Gwei total_balance = get_previous_total_balance(state);
    Gwei total_attesting_balance = get_attesting_balance(state, previous_epoch_attestations);
    List<PendingAttestation> matching_head_attestations =
        get_previous_epoch_matching_head_attestations(state);
    Gwei matching_head_balance = get_attesting_balance(state, matching_head_attestations);

    // Process rewards or penalties for all validators
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), get_previous_epoch(state));
    for (ValidatorIndex index : active_validator_indices) {
      int i = index.getIntValue();
      // Expected FFG source

      /* if index in get_attesting_indices(state, state.previous_epoch_attestations):
            deltas[0][index] += get_base_reward(state, index) * total_attesting_balance // total_balance
            # Inclusion speed bonus
            deltas[0][index] += (
                get_base_reward(state, index) * MIN_ATTESTATION_INCLUSION_DELAY //
                inclusion_distance(state, index)
            ) */
      if (get_attesting_indices(state, previous_epoch_attestations).contains(index)) {
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index).mulDiv(total_attesting_balance, total_balance));
        // Inclusion speed bonus
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index)
                .mulDiv(Gwei.castFrom(getConstants().getMinAttestationInclusionDelay()),
                    Gwei.castFrom(inclusion_distance(state, index))));
      } else {
        /* else:
             deltas[1][index] += get_base_reward(state, index) */
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }

      // Expected FFG target

      /* if index in get_attesting_indices(state, boundary_attestations):
           deltas[0][index] += get_base_reward(state, index) * boundary_attesting_balance // total_balance
         else:
           deltas[1][index] += get_base_reward(state, index) */
      if (get_attesting_indices(state, boundary_attestations).contains(index)) {
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index).mulDiv(boundary_attesting_balance, total_balance));
      } else {
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }

      // Expected head

      /* if index in get_attesting_indices(state, matching_head_attestations):
           deltas[0][index] += get_base_reward(state, index) * matching_head_balance // total_balance
         else:
           deltas[1][index] += get_base_reward(state, index) */
      if (get_attesting_indices(state, matching_head_attestations).contains(index)) {
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index).mulDiv(matching_head_balance, total_balance));
      } else {
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }

      // Proposer bonus
      /* if index in get_attesting_indices(state, state.previous_epoch_attestations):
            proposer_index = get_beacon_proposer_index(state, inclusion_slot(state, index))
            deltas[0][proposer_index] += get_base_reward(state, index) // ATTESTATION_INCLUSION_REWARD_QUOTIENT */
      if (get_attesting_indices(state, previous_epoch_attestations).contains(index)) {
        ValidatorIndex proposer_index = get_beacon_proposer_index(state, inclusion_slot(state, index));
        deltas[0][proposer_index.getIntValue()] = deltas[0][proposer_index.getIntValue()].plus(
            get_base_reward(state, index).dividedBy(getConstants().getAttestationInclusionRewardQuotient()));
      }
    }

    return deltas;
  }

  /*
    When blocks are not finalizing normally...

    # deltas[0] for rewards
    # deltas[1] for penalties
   */
  default Gwei[][] compute_inactivity_leak_deltas(BeaconState state) {
    /*
      deltas = [
        [0 for index in range(len(state.validator_registry))],
        [0 for index in range(len(state.validator_registry))]
      ] */
    Gwei[][] deltas = {
        new Gwei[state.getValidatorRegistry().size().getIntValue()],
        new Gwei[state.getValidatorRegistry().size().getIntValue()]
    };
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      deltas[0][index.getIntValue()] = deltas[1][index.getIntValue()] = Gwei.ZERO;
    }

    List<PendingAttestation> previous_epoch_attestations =
        state.getPreviousEpochAttestations().listCopy();
    List<PendingAttestation> boundary_attestations =
        get_previous_epoch_boundary_attestations(state);
    List<PendingAttestation> matching_head_attestations =
        get_previous_epoch_matching_head_attestations(state);
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), get_previous_epoch(state));
    EpochNumber epochs_since_finality =
        get_current_epoch(state).increment().minus(state.getFinalizedEpoch());

    // for index in active_validator_indices:
    for (ValidatorIndex index : active_validator_indices) {
      int i = index.getIntValue();

      /* if index not in get_attesting_indices(state, state.previous_epoch_attestations):
            deltas[1][index] += get_inactivity_penalty(state, index, epochs_since_finality)
        else:
            # If a validator did attest, apply a small penalty for getting attestations included late
            deltas[0][index] += (
                get_base_reward(state, index) * MIN_ATTESTATION_INCLUSION_DELAY //
                inclusion_distance(state, index)
            )
            deltas[1][index] += get_base_reward(state, index) */
      if (!get_attesting_indices(state, previous_epoch_attestations).contains(index)) {
        deltas[1][i] = deltas[1][i].plus(
            get_inactivity_penalty(state, index, epochs_since_finality));
      } else {
        // If a validator did attest, apply a small penalty for getting attestations included late
        deltas[0][i] = deltas[0][i].plus(
            get_base_reward(state, index).mulDiv(
                Gwei.castFrom(getConstants().getMinAttestationInclusionDelay()),
                Gwei.castFrom(inclusion_distance(state, index))));
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }

      /* if index not in get_attesting_indices(state, boundary_attestations):
            deltas[1][index] += get_inactivity_penalty(state, index, epochs_since_finality) */
      if (!get_attesting_indices(state, boundary_attestations).contains(index)) {
        deltas[1][i] = deltas[1][i].plus(get_inactivity_penalty(state, index, epochs_since_finality));
      }
      /* if index not in get_attesting_indices(state, matching_head_attestations):
            deltas[1][index] += get_base_reward(state, index) */
      if (!get_attesting_indices(state, matching_head_attestations).contains(index)) {
        deltas[1][i] = deltas[1][i].plus(get_base_reward(state, index));
      }
    }

    // Penalize slashed-but-inactive validators as though they were active but offline

    // for index in range(len(state.validator_registry)):
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      /* eligible = (
            index not in active_validator_indices and
            state.validator_registry[index].slashed and
            get_current_epoch(state) < state.validator_registry[index].withdrawable_epoch
        ) */
      boolean eligible = !active_validator_indices.contains(index) &&
          state.getValidatorRegistry().get(index).getSlashed() &&
          get_current_epoch(state).less(state.getValidatorRegistry().get(index).getWithdrawableEpoch());

      /* if eligible:
            deltas[1][index] += (
                2 * get_inactivity_penalty(state, index, epochs_since_finality) +
                get_base_reward(state, index)
            ) */
      if (eligible) {
        deltas[1][index.getIntValue()] = deltas[1][index.getIntValue()].plus(
            get_inactivity_penalty(state, index, epochs_since_finality).times(2)
                .plus(get_base_reward(state, index)));
      }
    }

    return deltas;
  }

  /*
    def get_justification_and_finalization_deltas(state: BeaconState) -> Tuple[List[Gwei], List[Gwei]]:
      epochs_since_finality = get_current_epoch(state) + 1 - state.finalized_epoch
      if epochs_since_finality <= 4:
          return compute_normal_justification_and_finalization_deltas(state)
      else:
          return compute_inactivity_leak_deltas(state)
   */
  default Gwei[][] get_justification_and_finalization_deltas(BeaconState state) {
    EpochNumber epochs_since_finality =
        get_current_epoch(state).increment().minus(state.getFinalizedEpoch());
    if (epochs_since_finality.lessEqual(EpochNumber.of(4))) {
      return compute_normal_justification_and_finalization_deltas(state);
    } else {
      return compute_inactivity_leak_deltas(state);
    }
  }

  /*
     # deltas[0] for rewards
     # deltas[1] for penalties
   */
  default Gwei[][] get_crosslink_deltas(BeaconState state) {
    /*
      deltas = [
        [0 for index in range(len(state.validator_registry))],
        [0 for index in range(len(state.validator_registry))]
      ] */
    Gwei[][] deltas = {
        new Gwei[state.getValidatorRegistry().size().getIntValue()],
        new Gwei[state.getValidatorRegistry().size().getIntValue()]
    };
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      deltas[0][index.getIntValue()] = deltas[1][index.getIntValue()] = Gwei.ZERO;
    }

    SlotNumber previous_epoch_start_slot = get_epoch_start_slot(get_previous_epoch(state));
    SlotNumber current_epoch_start_slot = get_epoch_start_slot(get_current_epoch(state));

    /* for slot in range(previous_epoch_start_slot, current_epoch_start_slot):
         for crosslink_committee, shard in get_crosslink_committees_at_slot(state, slot): */
    for (SlotNumber slot : previous_epoch_start_slot.iterateTo(current_epoch_start_slot)) {
      List<ShardCommittee> committees_and_shards = get_crosslink_committees_at_slot(state, slot);
      for (ShardCommittee committee_and_shard : committees_and_shards) {
        List<ValidatorIndex> crosslink_committee = committee_and_shard.getCommittee();
        ShardNumber shard = committee_and_shard.getShard();
        /*  winning_root, participants = get_winning_root_and_participants(state, shard)
            participating_balance = get_total_balance(state, participants)
            total_balance = get_total_balance(state, crosslink_committee) */
        Pair<Hash32, List<ValidatorIndex>> winning_root_and_participants =
            get_winning_root_and_participants(state, shard);
        Gwei participating_balance = get_total_balance(state, winning_root_and_participants.getValue1());
        Gwei total_balance = get_total_balance(state, crosslink_committee);

        /* for index in crosslink_committee:
              if index in participants:
                  deltas[0][index] += get_base_reward(state, index) * participating_balance // total_balance
              else:
                  deltas[1][index] += get_base_reward(state, index) */
        for (ValidatorIndex index : crosslink_committee) {
          if (winning_root_and_participants.getValue1().contains(index)) {
            deltas[0][index.getIntValue()] = deltas[0][index.getIntValue()].plus(
                get_base_reward(state, index).mulDiv(participating_balance, total_balance));
          } else {
            deltas[1][index.getIntValue()] = deltas[1][index.getIntValue()].plus(
                get_base_reward(state, index));
          }
        }
      }
    }

    return deltas;
  }

  /*
    Note: this function mutates beacon state.

    def apply_rewards(state: BeaconState) -> None:
      deltas1 = get_justification_and_finalization_deltas(state)
      deltas2 = get_crosslink_deltas(state)
      for i in range(len(state.validator_registry)):
          state.validator_balances[i] = max(
              0,
              state.validator_balances[i] + deltas1[0][i] + deltas2[0][i] - deltas1[1][i] - deltas2[1][i]
          )
   */
  default void apply_rewards(MutableBeaconState state) {
    Gwei[][] deltas1 = get_justification_and_finalization_deltas(state);
    Gwei[][] deltas2 = get_crosslink_deltas(state);
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      int i = index.getIntValue();
      state.getValidatorBalances().update(index, balance ->
          balance.plus(deltas1[0][i]).plus(deltas2[0][i])
              .minusSat(deltas1[1][i]).minusSat(deltas2[1][i]));
    }
  }

  /*
    def process_ejections(state: BeaconState) -> None:
      """
      Iterate through the validator registry
      and eject active validators with balance below ``EJECTION_BALANCE``.
      """
      for index in get_active_validator_indices(state.validator_registry, get_current_epoch(state)):
          if state.validator_balances[index] < EJECTION_BALANCE:
              exit_validator(state, index)
   */
  default void process_ejections(MutableBeaconState state) {
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), get_current_epoch(state));
    for (ValidatorIndex index : active_validator_indices) {
      if (state.getValidatorBalances().get(index).less(getConstants().getEjectionBalance())) {
        exit_validator(state, index);
      }
    }
  }

  /*
    def should_update_validator_registry(state: BeaconState) -> bool:
    # Must have finalized a new block
    if state.finalized_epoch <= state.validator_registry_update_epoch:
        return False
    # Must have processed new crosslinks on all shards of the current epoch
    shards_to_check = [
        (state.current_shuffling_start_shard + i) % SHARD_COUNT
        for i in range(get_current_epoch_committee_count(state))
    ]
    for shard in shards_to_check:
        if state.latest_crosslinks[shard].epoch <= state.validator_registry_update_epoch:
            return False
    return True
   */
  default boolean should_update_validator_registry(BeaconState state) {
    // Must have finalized a new block
    if (state.getFinalizedEpoch().lessEqual(state.getValidatorRegistryUpdateEpoch())) {
      return false;
    }
    // Must have processed new crosslinks on all shards of the current epoch
    List<ShardNumber> shards_to_check = IntStream.range(0, get_current_epoch_committee_count(state))
        .mapToObj(i -> ShardNumber.of(state.getCurrentShufflingStartShard()
            .plus(i).modulo(getConstants().getShardCount()))).collect(toList());
    for (ShardNumber shard : shards_to_check) {
      if (state.getLatestCrosslinks().get(shard).getEpoch()
          .lessEqual(state.getValidatorRegistryUpdateEpoch())) {
        return false;
      }
    }

    return true;
  }

  /*
    """
    Update validator registry.
    Note that this function mutates ``state``.
    """
   */
  default void update_validator_registry(MutableBeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    // The active validators
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), current_epoch);
    // The total effective balance of active validators
    Gwei total_balance = get_total_balance(state, active_validator_indices);

    // The maximum balance churn in Gwei (for deposits and exits separately)
    Gwei max_balance_churn = UInt64s.max(
        getConstants().getMaxDepositAmount(),
        total_balance.dividedBy(getConstants().getMaxBalanceChurnQuotient().times(2))
    );

    // Activate validators within the allowable balance churn

    /*  balance_churn = 0
        for index, validator in enumerate(state.validator_registry):
            if validator.activation_epoch == FAR_FUTURE_EPOCH and state.validator_balances[index] >= MAX_DEPOSIT_AMOUNT:
                # Check the balance churn would be within the allowance
                balance_churn += get_effective_balance(state, index)
                if balance_churn > max_balance_churn:
                    break

                # Activate validator
                activate_validator(state, index, is_genesis=False) */
    Gwei balance_churn = Gwei.ZERO;
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getActivationEpoch().equals(getConstants().getFarFutureEpoch()) &&
          state.getValidatorBalances().get(index).greaterEqual(getConstants().getMaxDepositAmount())) {

        // Check the balance churn would be within the allowance
        balance_churn = balance_churn.plus(get_effective_balance(state, index));
        if (balance_churn.greater(max_balance_churn)) {
          break;
        }

        // Activate validator
        activate_validator(state, index, false);
      }
    }

    // Exit validators within the allowable balance churn

    /*  balance_churn = 0
        for index, validator in enumerate(state.validator_registry):
            if validator.exit_epoch == FAR_FUTURE_EPOCH and validator.initiated_exit:
                # Check the balance churn would be within the allowance
                balance_churn += get_effective_balance(state, index)
                if balance_churn > max_balance_churn:
                    break

                # Exit validator
                exit_validator(state, index) */
    balance_churn = Gwei.ZERO;
    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getExitEpoch().equals(getConstants().getFarFutureEpoch()) &&
          validator.getInitiatedExit()) {
        // Check the balance churn would be within the allowance
        balance_churn = balance_churn.plus(get_effective_balance(state, index));
        if (balance_churn.greater(max_balance_churn)) {
          break;
        }

        // Exit validator
        exit_validator(state, index);
      }
    }

    state.setValidatorRegistryUpdateEpoch(current_epoch);
  }

  default void update_registry_and_shuffling_data(MutableBeaconState state) {
    // First set previous shuffling data to current shuffling data
    state.setPreviousShufflingEpoch(state.getCurrentShufflingEpoch());
    state.setPreviousShufflingStartShard(state.getCurrentShufflingStartShard());
    state.setPreviousShufflingSeed(state.getCurrentShufflingSeed());
    EpochNumber current_epoch = get_current_epoch(state);
    EpochNumber next_epoch = current_epoch.increment();

    // Check if we should update, and if so, update
    if (should_update_validator_registry(state)) {
      /* update_validator_registry(state)
        # If we update the registry, update the shuffling data and shards as well
        state.current_shuffling_epoch = next_epoch
        state.current_shuffling_start_shard = (
            state.current_shuffling_start_shard +
            get_current_epoch_committee_count(state)
        ) % SHARD_COUNT
        state.current_shuffling_seed = generate_seed(state, state.current_shuffling_epoch) */
      update_validator_registry(state);
      // If we update the registry, update the shuffling data and shards as well
      state.setCurrentShufflingEpoch(next_epoch);
      state.setCurrentShufflingStartShard(ShardNumber.of(
          state.getCurrentShufflingStartShard().plus(get_current_epoch_committee_count(state))
              .modulo(getConstants().getShardCount())));
      state.setCurrentShufflingSeed(generate_seed(state, state.getCurrentShufflingEpoch()));
    } else {
      // If processing at least one crosslink keeps failing, then reshuffle every power of two,
      // but don't update the current_shuffling_start_shard

      /* epochs_since_last_registry_update = current_epoch - state.validator_registry_update_epoch
        if epochs_since_last_registry_update > 1 and is_power_of_two(epochs_since_last_registry_update):
            state.current_shuffling_epoch = next_epoch
            state.current_shuffling_seed = generate_seed(state, state.current_shuffling_epoch) */
      EpochNumber epochs_since_last_registry_update = current_epoch.minus(
          state.getValidatorRegistryUpdateEpoch());
      if (epochs_since_last_registry_update.greater(EpochNumber.of(1)) &&
          is_power_of_two(epochs_since_last_registry_update)) {
        state.setCurrentShufflingEpoch(next_epoch);
        state.setCurrentShufflingSeed(generate_seed(state, state.getCurrentShufflingEpoch()));
      }
    }
  }

  /*
    """
    Process the slashings.
    Note that this function mutates ``state``.
    """
   */
  default void process_slashings(MutableBeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state.getValidatorRegistry(), current_epoch);
    Gwei total_balance = get_total_balance(state, active_validator_indices);

    // Compute `total_penalties`
    Gwei total_at_start = state.getLatestSlashedBalances().get(current_epoch.increment()
        .modulo(getConstants().getLatestSlashedExitLength()));
    Gwei total_at_end = state.getLatestSlashedBalances()
        .get(current_epoch.modulo(getConstants().getLatestSlashedExitLength()));
    Gwei total_penalties = total_at_end.minusSat(total_at_start);

    /* for index, validator in enumerate(state.validator_registry):
        if validator.slashed and current_epoch == validator.withdrawable_epoch - LATEST_SLASHED_EXIT_LENGTH // 2:
            penalty = max(
                get_effective_balance(state, index) * min(total_penalties * 3, total_balance) // total_balance,
                get_effective_balance(state, index) // MIN_PENALTY_QUOTIENT
            )
            state.validator_balances[index] -= penalty */

    for (ValidatorIndex index : state.getValidatorRegistry().size()) {
      ValidatorRecord validator = state.getValidatorRegistry().get(index);
      if (validator.getSlashed() &&
          current_epoch.equals(validator.getWithdrawableEpoch()
              .minus(getConstants().getLatestSlashedExitLength().half()))) {
        Gwei effective_balance = get_effective_balance(state, index);
        Gwei penalty = UInt64s.max(
            effective_balance.times(UInt64s.min(total_penalties.times(3), total_balance).dividedBy(total_balance)),
            effective_balance.dividedBy(getConstants().getMinPenaltyQuotient())
        );
        state.getValidatorBalances().update(index, balance -> balance.minusSat(penalty));
      }
    }
  }

  /*
    def eligible(index):
      validator = state.validator_registry[index]
      # Filter out dequeued validators
      if validator.withdrawable_epoch != FAR_FUTURE_EPOCH:
          return False
      # Dequeue if the minimum amount of time has passed
      else:
          return get_current_epoch(state) >= validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY
   */
  default boolean eligible(BeaconState state, ValidatorIndex index) {
    ValidatorRecord validator = state.getValidatorRegistry().get(index);
    // Filter out dequeued validators
    if (!validator.getWithdrawableEpoch().equals(getConstants().getFarFutureEpoch())) {
      return false;
    } else {
      // Dequeue if the minimum amount of time has passed
      return get_current_epoch(state).greaterEqual(
          validator.getExitEpoch().plus(getConstants().getMinValidatorWithdrawabilityDelay()));
    }
  }

  /*
    """
    Process the exit queue.
    Note that this function mutates ``state``.
    """
   */
  default void process_exit_queue(MutableBeaconState state) {
    // eligible_indices = filter(eligible, list(range(len(state.validator_registry))))
    // Sort in order of exit epoch,
    // and validators that exit within the same epoch exit in order of validator index
    List<ValidatorIndex> sorted_eligible_indices =
        StreamSupport.stream(state.getValidatorRegistry().size().spliterator(), false)
            .filter(index -> eligible(state, index))
            .sorted(Comparator.comparing(index -> state.getValidatorRegistry().get(index).getExitEpoch()))
            .collect(toList());

    /* for dequeues, index in enumerate(sorted_indices):
        if dequeues >= MAX_EXIT_DEQUEUES_PER_EPOCH:
            break
        prepare_validator_for_withdrawal(state, index) */
    for (int i = 0; i < sorted_eligible_indices.size(); i++) {
      int dequeues = i;
      if (dequeues >= getConstants().getMaxExitDequesPerEpoch().getIntValue()) {
        break;
      }
      prepare_validator_for_withdrawal(state, sorted_eligible_indices.get(i));
    }
  }

  default void finish_epoch_update(MutableBeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    EpochNumber next_epoch = current_epoch.increment();

    // Set active index root
    EpochNumber index_root_position = next_epoch
        .plus(getConstants().getActivationExitDelay()).modulo(getConstants().getLatestActiveIndexRootsLength());
    state.getLatestActiveIndexRoots().set(index_root_position, hash_tree_root(
        get_active_validator_indices(state.getValidatorRegistry(),
            next_epoch.plus(getConstants().getActivationExitDelay()))));

    // Set total slashed balances
    state.getLatestSlashedBalances().set(next_epoch.modulo(getConstants().getLatestSlashedExitLength()),
        state.getLatestSlashedBalances().get(
            current_epoch.modulo(getConstants().getLatestSlashedExitLength())));

    // Set randao mix
    state.getLatestRandaoMixes().set(next_epoch.modulo(getConstants().getLatestRandaoMixesLength()),
        get_randao_mix(state, current_epoch));

    // Set historical root accumulator
    if (next_epoch.modulo(getConstants().getSlotsPerHistoricalRoot().dividedBy(getConstants().getSlotsPerEpoch()))
        .equals(EpochNumber.ZERO)) {
      HistoricalBatch historical_batch =
          new HistoricalBatch(
              state.getLatestBlockRoots().listCopy(),
              state.getLatestStateRoots().listCopy());
      state.getHistoricalRoots().add(hash_tree_root(historical_batch));
    }

    // Rotate current/previous epoch attestations
    state.getPreviousEpochAttestations().clear();
    state.getPreviousEpochAttestations().addAll(state.getCurrentEpochAttestations().listCopy());
    state.getCurrentEpochAttestations().clear();
  }

  default void process_block_header(MutableBeaconState state, BeaconBlock block) {
    // Verify that the slots match
    assertTrue(block.getSlot().equals(state.getSlot()));
    // Verify that the parent matches
    assertTrue(block.getPreviousBlockRoot().equals(signed_root(state.getLatestBlockHeader())));
    // Save current block as the new latest block
    state.setLatestBlockHeader(get_temporary_block_header(block));
  }

  default void process_randao(MutableBeaconState state, BeaconBlock block) {
    // Mix it in
    state.getLatestRandaoMixes().set(get_current_epoch(state).modulo(getConstants().getLatestRandaoMixesLength()),
        Hash32.wrap(Bytes32s.xor(
            get_randao_mix(state, get_current_epoch(state)),
            hash(block.getBody().getRandaoReveal()))));
  }

  default void process_eth1_data(MutableBeaconState state, BeaconBlock block) {
    /* for eth1_data_vote in state.eth1_data_votes:
        # If someone else has already voted for the same hash, add to its counter
        if eth1_data_vote.eth1_data == block.body.eth1_data:
            eth1_data_vote.vote_count += 1
            return */
    for (int i = 0; i < state.getEth1DataVotes().size(); i++) {
      Eth1DataVote eth1_data_vote = state.getEth1DataVotes().get(i);
      // If someone else has already voted for the same hash, add to its counter
      if (eth1_data_vote.getEth1Data().equals(block.getBody().getEth1Data())) {
        state.getEth1DataVotes().update(i, vote ->
            new Eth1DataVote(vote.getEth1Data(), vote.getVoteCount().increment()));
        return;
      }
    }

    // If we're seeing this hash for the first time, make a new counter
    state.getEth1DataVotes().add(
        new Eth1DataVote(block.getBody().getEth1Data(), UInt64.valueOf(1)));
  }

  /*
    """
    Process ``ProposerSlashing`` transaction.
    Note that this function mutates ``state``.
    """
   */
  default void process_proposer_slashing(MutableBeaconState state, ProposerSlashing proposer_slashing) {
    slash_validator(state, proposer_slashing.getProposerIndex());
  }

  /*
    """
    Process ``AttesterSlashing`` transaction.
    Note that this function mutates ``state``.
    """
   */
  default void process_attester_slashing(MutableBeaconState state, AttesterSlashing attester_slashing) {
    List<ValidatorIndex> slashable_indices =
        attester_slashing.getSlashableAttestation1().getValidatorIndices().intersection(
            attester_slashing.getSlashableAttestation2().getValidatorIndices()).stream()
            .filter(index -> !state.getValidatorRegistry().get(index).getSlashed())
            .collect(toList());

    for (ValidatorIndex index : slashable_indices) {
      slash_validator(state, index);
    }
  }

  /*
   """
   Process ``Attestation`` transaction.
   Note that this function mutates ``state``.
   """
  */
  default void process_attestation(MutableBeaconState state, Attestation attestation) {
    // Apply the attestation
    PendingAttestation pending_attestation = new PendingAttestation(
        attestation.getAggregationBitfield(),
        attestation.getData(),
        attestation.getCustodyBitfield(),
        state.getSlot()
    );

    if (slot_to_epoch(attestation.getData().getSlot()).equals(get_current_epoch(state))) {
      state.getCurrentEpochAttestations().add(pending_attestation);
    } else if (slot_to_epoch(attestation.getData().getSlot()).equals(get_previous_epoch(state))) {
      state.getPreviousEpochAttestations().add(pending_attestation);
    }
  }

  /*
    """
    Process ``VoluntaryExit`` transaction.
    Note that this function mutates ``state``.
    """
   */
  default void process_voluntary_exit(MutableBeaconState state, VoluntaryExit exit) {
    initiate_validator_exit(state, exit.getValidatorIndex());
  }

  /*
    """
    Process ``Transfer`` transaction.
    Note that this function mutates ``state``.
    """
   */
  default void process_transfer(MutableBeaconState state, Transfer transfer) {
    // Process the transfer
    state.getValidatorBalances().update(transfer.getSender(),
        balance -> balance.minusSat(transfer.getAmount().plus(transfer.getFee())));
    state.getValidatorBalances().update(transfer.getRecipient(),
        balance -> balance.plusSat(transfer.getAmount()));
    state.getValidatorBalances().update(get_beacon_proposer_index(state, state.getSlot()),
        balance -> balance.plusSat(transfer.getFee()));
  }

  static void assertTrue(boolean assertion) {
    if (!assertion) {
      throw new SpecAssertionFailed();
    }
  }

  class SpecAssertionFailed extends RuntimeException {}
}

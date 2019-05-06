package org.ethereum.beacon.consensus.spec;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
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
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.0/specs/core/0_beacon-chain.md#helper-functions">Helper
 *     functions</a> in ths spec.
 */
public interface HelperFunction extends SpecCommons {

  default Hash32 hash(BytesValue data) {
    return getHashFunction().apply(data);
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
         # signing_root(block) is used for block id purposes so signature is a stub
         signature=BLSSignature.ZERO,
     )
  */
  default BeaconBlockHeader get_temporary_block_header(BeaconBlock block) {
    return new BeaconBlockHeader(
        block.getSlot(),
        block.getPreviousBlockRoot(),
        Hash32.ZERO,
        hash_tree_root(block.getBody()),
        BLSSignature.ZERO);
  }

  /*
    def get_epoch_committee_count(state: BeaconState, epoch: Epoch) -> int:
      """
      Return the number of committees at ``epoch``.
      """
      active_validator_indices = get_active_validator_indices(state, epoch)
      return max(
          1,
          min(
              SHARD_COUNT // SLOTS_PER_EPOCH,
              len(active_validator_indices) // SLOTS_PER_EPOCH // TARGET_COMMITTEE_SIZE,
          )
      ) * SLOTS_PER_EPOCH
   */
  default int get_epoch_committee_count(BeaconState state, EpochNumber epoch) {
    List<ValidatorIndex> active_validator_indices = get_active_validator_indices(state, epoch);

    return UInt64s.max(
        UInt64.valueOf(1),
        UInt64s.min(
            getConstants().getShardCount().dividedBy(getConstants().getSlotsPerEpoch()),
            UInt64.valueOf(active_validator_indices.size())
                .dividedBy(getConstants().getSlotsPerEpoch())
                .dividedBy(getConstants().getTargetCommitteeSize())
        )).times(getConstants().getSlotsPerEpoch()).intValue();
  }

  /*
    def get_shard_delta(state: BeaconState, epoch: Epoch) -> int:
      """
      Return the number of shards to increment ``state.latest_start_shard`` during ``epoch``.
      """
      return min(get_epoch_committee_count(state, epoch), SHARD_COUNT - SHARD_COUNT // SLOTS_PER_EPOCH)
   */
  default int get_shard_delta(BeaconState state, EpochNumber epoch) {
    return Math.min(
        get_epoch_committee_count(state, epoch),
        getConstants().getShardCount().minus(
            getConstants().getShardCount().dividedBy(getConstants().getSlotsPerEpoch())
        ).getIntValue()
    );
  }

  /*
    def compute_committee(validator_indices: List[ValidatorIndex],
                      seed: Bytes32,
                      index: int,
                      total_committees: int) -> List[ValidatorIndex]:
      """
      Return the ``index``'th shuffled committee out of a total ``total_committees``
      using ``validator_indices`` and ``seed``.
      """
      start_offset = get_split_offset(len(validator_indices), total_committees, index)
      end_offset = get_split_offset(len(validator_indices), total_committees, index + 1)
      return [
          validator_indices[get_permuted_index(i, len(validator_indices), seed)]
          for i in range(start_offset, end_offset)
      ]
   */
  default List<ValidatorIndex> compute_committee(
      List<ValidatorIndex> validator_indices, Hash32 seed, int index, int total_committees) {
    int start_offset = get_split_offset(validator_indices.size(), total_committees, index);
    int end_offset = get_split_offset(validator_indices.size(), total_committees, index + 1);

    return compute_committee(validator_indices, start_offset, end_offset, seed);
  }

  default List<ValidatorIndex> compute_committee(
      List<ValidatorIndex> validator_indices, int start_offset, int end_offset, Hash32 seed) {
    return IntStream.range(start_offset, end_offset).mapToObj(i -> {
      UInt64 permuted_index =
          get_permuted_index(UInt64.valueOf(i), UInt64.valueOf(validator_indices.size()), seed);
      return validator_indices.get(permuted_index.getIntValue());
    }).collect(toList());
  }

  /**
   * An optimized version of {@link #compute_committee(List, Hash32, int, int)}.
   * Based on {@link #get_permuted_list(List, Bytes32)}.
   */
  default List<ValidatorIndex> compute_committee2(
      List<ValidatorIndex> validator_indices, Hash32 seed, int index, int total_committees) {
    int start_offset = get_split_offset(validator_indices.size(), total_committees, index);
    int end_offset = get_split_offset(validator_indices.size(), total_committees, index + 1);

    return compute_committee2(validator_indices, start_offset, end_offset, seed);
  }

  default List<ValidatorIndex> compute_committee2(
      List<ValidatorIndex> validator_indices, int start_offset, int end_offset, Hash32 seed) {
    List<UInt64> permuted_indices = get_permuted_list(validator_indices, seed);

    return permuted_indices.subList(start_offset, end_offset).stream()
        .map(ValidatorIndex::new).collect(toList());
  }

  /*
    def get_crosslink_committees_at_slot(state: BeaconState,
                                     slot: Slot) -> List[Tuple[List[ValidatorIndex], Shard]]:
      """
      Return the list of ``(committee, shard)`` tuples for the ``slot``.
      """
   */
  default List<ShardCommittee> get_crosslink_committees_at_slot(
      BeaconState state, SlotNumber slot) {
    EpochNumber epoch = slot_to_epoch(slot);
    EpochNumber currentEpoch = get_current_epoch(state);
    EpochNumber previousEpoch = get_previous_epoch(state);
    EpochNumber nextEpoch = currentEpoch.increment();

    assertTrue(previousEpoch.lessEqual(epoch) && epoch.lessEqual(nextEpoch));
    List<ValidatorIndex> indices = get_active_validator_indices(state, epoch);

    ShardNumber start_shard;
    if (epoch.equals(currentEpoch)) {
      /*
        if epoch == current_epoch:
          start_shard = state.latest_start_shard */

      start_shard = state.getLatestStartShard();
    } else if (epoch.equals(previousEpoch)) {
      /*
        elif epoch == previous_epoch:
          previous_shard_delta = get_shard_delta(state, previous_epoch)
          start_shard = (state.latest_start_shard - previous_shard_delta) % SHARD_COUNT */

      int previous_shard_delta = get_shard_delta(state, previousEpoch);
      start_shard = ShardNumber.of(state.getLatestStartShard()
          .minusModulo(previous_shard_delta, getConstants().getShardCount()));
    } else if (epoch.equals(nextEpoch)) {
      /*
        elif epoch == next_epoch:
          current_shard_delta = get_shard_delta(state, current_epoch)
          start_shard = (state.latest_start_shard + current_shard_delta) % SHARD_COUNT */
      int current_shard_delta = get_shard_delta(state, currentEpoch);
      start_shard = ShardNumber.of(state.getLatestStartShard()
          .plusModulo(current_shard_delta, getConstants().getShardCount()));
    } else {
      throw new BeaconChainSpec.SpecAssertionFailed();
    }

    /*
      committees_per_epoch = get_epoch_committee_count(state, epoch)
      committees_per_slot = committees_per_epoch // SLOTS_PER_EPOCH
      offset = slot % SLOTS_PER_EPOCH
      slot_start_shard = (start_shard + committees_per_slot * offset) % SHARD_COUNT
      seed = generate_seed(state, epoch)
     */
    int committees_per_epoch = get_epoch_committee_count(state, epoch);
    int committees_per_slot = committees_per_epoch / getConstants().getSlotsPerEpoch().getIntValue();
    int offset = slot.modulo(getConstants().getSlotsPerEpoch()).getIntValue();
    ShardNumber slot_start_shard = start_shard
        .plusModulo(committees_per_slot * offset, getConstants().getShardCount());
    Hash32 seed = generate_seed(state, epoch);

    /*
      return [
        (
            compute_committee(indices, seed, committees_per_slot * offset + i, committees_per_epoch),
            (slot_start_shard + i) % SHARD_COUNT,
        )
        for i in range(committees_per_slot)
      ]
     */
    List<ShardCommittee> ret = new ArrayList<>();
    for(int i = 0; i < committees_per_slot; i++) {
      ShardCommittee committee = new ShardCommittee(
          compute_committee2(indices, seed, committees_per_slot * offset + i, committees_per_epoch),
          slot_start_shard.plusModulo(i, getConstants().getShardCount()));
      ret.add(committee);
    }

    return ret;
  }

  /*
    def get_beacon_proposer_index(state: BeaconState) -> ValidatorIndex:
      """
      Return the beacon proposer index at ``state.slot``.
      """
      current_epoch = get_current_epoch(state)
      first_committee, _ = get_crosslink_committees_at_slot(state, state.slot)[0]
      MAX_RANDOM_BYTE = 2**8 - 1
      i = 0
      while True:
          candidate_index = first_committee[(current_epoch + i) % len(first_committee)]
          random_byte = hash(generate_seed(state, current_epoch) + int_to_bytes8(i // 32))[i % 32]
          effective_balance = state.validator_registry[candidate_index].effective_balance
          if effective_balance * MAX_RANDOM_BYTE >= MAX_EFFECTIVE_BALANCE * random_byte:
              return candidate_index
          i += 1
    */
  int MAX_RANDOM_BYTE = (1 << 8) - 1;
  default ValidatorIndex get_beacon_proposer_index(BeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    List<ValidatorIndex> first_committee =
        get_crosslink_committees_at_slot(state, state.getSlot()).get(0).getCommittee();
    int i = 0;
    while (true) {
      ValidatorIndex candidate_index = first_committee.get(
          current_epoch.plus(i).modulo(first_committee.size()).getIntValue());
      int random_byte = hash(generate_seed(state, current_epoch)
          .concat(int_to_bytes8(i / Bytes32.SIZE)))
          .get(i % Bytes32.SIZE) & 0xFF;
      Gwei effective_balance = state.getValidatorRegistry().get(candidate_index).getEffectiveBalance();
      if (effective_balance.times(MAX_RANDOM_BYTE).greaterEqual(
          getConstants().getMaxEffectiveBalance().times(random_byte))) {
        return candidate_index;
      }
      i += 1;
    }
  }

  /*
    def is_slashable_validator(validator: Validator, epoch: Epoch) -> bool:
      """
      Check if ``validator`` is slashable.
      """
      return validator.slashed is False and (validator.activation_epoch <= epoch < validator.withdrawable_epoch)
   */
  default boolean is_slashable_validator(ValidatorRecord validator, EpochNumber epoch) {
    return !validator.getSlashed()
        && validator.getActivationEpoch().lessEqual(epoch)
        && epoch.less(validator.getWithdrawableEpoch());
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
    def get_active_validator_indices(state: BeaconState, epoch: Epoch) -> List[ValidatorIndex]:
      """
      Get active validator indices at ``epoch``.
      """
      return [i for i, v in enumerate(state.validator_registry) if is_active_validator(v, epoch)]
    */
  default List<ValidatorIndex> get_active_validator_indices(BeaconState state, EpochNumber epoch) {
    ArrayList<ValidatorIndex> ret = new ArrayList<>();
    for (ValidatorIndex i : state.getValidatorRegistry().size()) {
      if (is_active_validator(state.getValidatorRegistry().get(i), epoch)) {
        ret.add(i);
      }
    }
    return ret;
  }

  /*
    def increase_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei) -> None:
      """
      Increase validator balance by ``delta``.
      """
      state.balances[index] += delta
   */
  default void increase_balance(MutableBeaconState state, ValidatorIndex index, Gwei delta) {
    state.getBalances().update(index, balance -> balance.plus(delta));
  }

  /*
    def decrease_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei) -> None:
      """
      Decrease validator balance by ``delta`` with underflow protection.
      """
      state.balances[index] = 0 if delta > state.balances[index] else state.balances[index] - delta
   */
  default void decrease_balance(MutableBeaconState state, ValidatorIndex index, Gwei delta) {
    if (delta.greater(state.getBalances().get(index))) {
      state.getBalances().update(index, balance -> Gwei.ZERO);
    } else {
      state.getBalances().update(index, balance -> balance.minus(delta));
    }
  }

  /*
    def get_randao_mix(state: BeaconState,
                   epoch: Epoch) -> Bytes32:
      """
      Return the randao mix at a recent ``epoch``.
      ``epoch`` expected to be between (current_epoch - LATEST_RANDAO_MIXES_LENGTH, current_epoch].
      """
      return state.latest_randao_mixes[epoch % LATEST_RANDAO_MIXES_LENGTH]
    */
  default Hash32 get_randao_mix(BeaconState state, EpochNumber epoch) {
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

  default Bytes8 int_to_bytes8(long value) {
    return Bytes8.longToBytes8LittleEndian(value);
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
    def get_split_offset(list_size: int, chunks: int, index: int) -> int:
      """
      Returns a value such that for a list L, chunk count k and index i,
      split(L, k)[i] == L[get_split_offset(len(L), k, i): get_split_offset(len(L), k, i+1)]
      """
      return (list_size * index) // chunks
   */
  default int get_split_offset(int list_size, int chunks, int index) {
    return (list_size * index) / chunks;
  }

  /**
   * An optimized version of calculating shuffled list.
   * Based on {@link #get_permuted_list(List, Bytes32)}.
   */
  default List<List<ValidatorIndex>> get_shuffling2(Hash32 seed,
      BeaconState state, EpochNumber epoch) {
    List<ValidatorIndex> active_validator_indices = get_active_validator_indices(state, epoch);
    List<ValidatorIndex> shuffled_indices = get_permuted_list(active_validator_indices, seed)
        .stream().map(ValidatorIndex::new).collect(toList());
    return split(shuffled_indices, get_epoch_committee_count(state, epoch));
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
        state.getBalances().get(validatorIdx),
        getConstants().getMaxEffectiveBalance());
  }

  /*
    def get_total_balance(state: BeaconState, indices: List[ValidatorIndex]) -> Gwei:
      """
      Return the combined effective balance of an array of ``validators``.
      """
      return sum([state.validator_registry[index].effective_balance for index in indices])
   */
  default Gwei get_total_balance(BeaconState state, Collection<ValidatorIndex> indices) {
    return indices.stream()
        .map(index -> state.getValidatorRegistry().get(index).getEffectiveBalance())
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
    def get_churn_limit(state: BeaconState) -> int:
      return max(
          MIN_PER_EPOCH_CHURN_LIMIT,
          len(get_active_validator_indices(state, get_current_epoch(state))) // CHURN_LIMIT_QUOTIENT
      )
   */
  default UInt64 get_churn_limit(BeaconState state) {
    return UInt64s.max(
        getConstants().getMinPerEpochChurnLimit(),
        UInt64.valueOf(get_active_validator_indices(state, get_current_epoch(state)).size())
            .dividedBy(getConstants().getChurnLimitQuotient())
    );
  }

  /*
    def slash_validator(state: BeaconState, slashed_index: ValidatorIndex, whistleblower_index: ValidatorIndex=None) -> None:
      """
      Slash the validator with index ``slashed_index``.
      Note that this function mutates ``state``.
      """
      current_epoch = get_current_epoch(state)
      initiate_validator_exit(state, slashed_index)
      state.validator_registry[slashed_index].slashed = True
      state.validator_registry[slashed_index].withdrawable_epoch = current_epoch + LATEST_SLASHED_EXIT_LENGTH
      slashed_balance = state.validator_registry[slashed_index].effective_balance
      state.latest_slashed_balances[current_epoch % LATEST_SLASHED_EXIT_LENGTH] += slashed_balance

      proposer_index = get_beacon_proposer_index(state)
      if whistleblower_index is None:
          whistleblower_index = proposer_index
      whistleblowing_reward = slashed_balance // WHISTLEBLOWING_REWARD_QUOTIENT
      proposer_reward = whistleblowing_reward // PROPOSER_REWARD_QUOTIENT
      increase_balance(state, proposer_index, proposer_reward)
      increase_balance(state, whistleblower_index, whistleblowing_reward - proposer_reward)
      decrease_balance(state, slashed_index, whistleblowing_reward)
    */
  default void slash_validator(MutableBeaconState state, ValidatorIndex slashed_index, ValidatorIndex whistleblower_index) {
    EpochNumber current_epoch = get_current_epoch(state);
    initiate_validator_exit(state, slashed_index);
    state.getValidatorRegistry().update(slashed_index,
        validator -> ValidatorRecord.Builder.fromRecord(validator)
            .withSlashed(Boolean.TRUE)
            .withWithdrawableEpoch(current_epoch.plus(getConstants().getLatestSlashedExitLength())).build());
    Gwei slashed_balance = state.getValidatorRegistry().get(slashed_index).getEffectiveBalance();
    state.getLatestSlashedBalances().update(current_epoch.modulo(getConstants().getLatestSlashedExitLength()),
        balance -> balance.plus(slashed_balance));

    ValidatorIndex proposer_index = get_beacon_proposer_index(state);
    if (whistleblower_index == null) {
      whistleblower_index = proposer_index;
    }
    Gwei whistleblowing_reward = slashed_balance.dividedBy(getConstants().getWhistleblowingRewardQuotient());
    Gwei proposer_reward = whistleblowing_reward.dividedBy(getConstants().getProposerRewardQuotient());
    increase_balance(state, proposer_index, proposer_reward);
    increase_balance(state, whistleblower_index, whistleblowing_reward);
    decrease_balance(state, slashed_index, whistleblowing_reward);
  }

  default void slash_validator(MutableBeaconState state, ValidatorIndex slashed_index) {
    slash_validator(state, slashed_index, null);
  }

  /*
    def initiate_validator_exit(state: BeaconState, index: ValidatorIndex) -> None:
      """
      Initiate the validator of the given ``index``.
      Note that this function mutates ``state``.
      """
  */
  default void initiate_validator_exit(MutableBeaconState state, ValidatorIndex index) {
    /* # Return if validator already initiated exit
      validator = state.validator_registry[index]
      if validator.exit_epoch != FAR_FUTURE_EPOCH:
          return */
    checkIndexRange(state, index);
    if (!state.getValidatorRegistry().get(index).getExitEpoch().equals(getConstants().getFarFutureEpoch())) {
      return;
    }

    /* # Compute exit queue epoch
      exit_epochs = [v.exit_epoch for v in state.validator_registry if v.exit_epoch != FAR_FUTURE_EPOCH]
      exit_queue_epoch = max(exit_epochs + [get_delayed_activation_exit_epoch(get_current_epoch(state))])
      exit_queue_churn = len([v for v in state.validator_registry if v.exit_epoch == exit_queue_epoch])
      if exit_queue_churn >= get_churn_limit(state):
          exit_queue_epoch += 1 */
    EpochNumber exit_queue_epoch = Stream.concat(
        state.getValidatorRegistry().stream()
            .filter(v -> !v.getExitEpoch().equals(getConstants().getFarFutureEpoch()))
            .map(ValidatorRecord::getExitEpoch),
        Stream.of(get_delayed_activation_exit_epoch(get_current_epoch(state)))
    ).max(EpochNumber::compareTo).get();

    long exit_queue_churn = state.getValidatorRegistry().stream()
        .filter(v -> v.getExitEpoch().equals(exit_queue_epoch))
        .count();
    if (UInt64.valueOf(exit_queue_churn).compareTo(get_churn_limit(state)) >= 0) {
      exit_queue_epoch.increment();
    }

    /* # Set validator exit epoch and withdrawable epoch
      validator.exit_epoch = exit_queue_epoch
      validator.withdrawable_epoch = validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY */
    state.getValidatorRegistry().update(index,
        validator -> ValidatorRecord.Builder.fromRecord(validator)
            .withExitEpoch(exit_queue_epoch)
            .withWithdrawableEpoch(validator.getExitEpoch().plus(getConstants().getMinValidatorWithdrawabilityDelay()))
            .build());
  }

  /** Function for hashing objects into a single root utilizing a hash tree structure */
  default Hash32 hash_tree_root(Object object) {
    return getObjectHasher().getHash(object);
  }

  /** Function for hashing self-signed objects */
  default Hash32 signing_root(Object object) {
    return getObjectHasher().getHashTruncateLast(object);
  }

  /*
    def get_active_index_root(state: BeaconState,
                          epoch: Epoch) -> Bytes32:
      """
      Return the index root at a recent ``epoch``.
      ``epoch`` expected to be between
      (current_epoch - LATEST_ACTIVE_INDEX_ROOTS_LENGTH + ACTIVATION_EXIT_DELAY, current_epoch + ACTIVATION_EXIT_DELAY].
      """
      return state.latest_active_index_roots[epoch % LATEST_ACTIVE_INDEX_ROOTS_LENGTH]
   */
  default Hash32 get_active_index_root(BeaconState state, EpochNumber epoch) {
    return state.getLatestActiveIndexRoots().get(
        epoch.modulo(getConstants().getLatestActiveIndexRootsLength()));
  }

  /*
    def generate_seed(state: BeaconState,
                  epoch: Epoch) -> Bytes32:
      """
      Generate a seed for the given ``epoch``.
      """
      return hash(
          get_randao_mix(state, epoch + LATEST_RANDAO_MIXES_LENGTH - MIN_SEED_LOOKAHEAD) +
          get_active_index_root(state, epoch) +
          int_to_bytes32(epoch)
      )
   */
  default Hash32 generate_seed(BeaconState state, EpochNumber epoch) {
    EpochNumber randao_mix_epoch = epoch
        .plus(getConstants().getLatestRandaoMixesLength())
        .minus(getConstants().getMinSeedLookahead());
    return hash(
        get_randao_mix(state, randao_mix_epoch)
        .concat(get_active_index_root(state, epoch))
        .concat(int_to_bytes32(epoch))
    );
  }

  default boolean bls_verify(BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    if (!isBlsVerify()) {
      return true;
    }

    PublicKey blsPublicKey = PublicKey.create(publicKey);
    MessageParameters messageParameters = MessageParameters.create(message, domain);
    Signature blsSignature = Signature.create(signature);
    return BLS381.verify(messageParameters, blsSignature, blsPublicKey);
  }

  default boolean bls_verify_multiple(
      List<PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, UInt64 domain) {
    if (!isBlsVerify()) {
      return true;
    }

    List<MessageParameters> messageParameters =
        messages.stream()
            .map(hash -> MessageParameters.create(hash, domain))
            .collect(Collectors.toList());
    Signature blsSignature = Signature.create(signature);
    return BLS381.verifyMultiple(messageParameters, blsSignature, publicKeys);
  }

  default PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
    if (!isBlsVerify()) {
      return PublicKey.aggregate(Collections.emptyList());
    }

    List<PublicKey> publicKeys = publicKeysBytes.stream().map(PublicKey::create).collect(toList());
    return PublicKey.aggregate(publicKeys);
  }

  /*
    def get_domain(state: BeaconState,
               domain_type: int,
               message_epoch: int=None) -> int:
      """
      Return the signature domain (fork version concatenated with domain type) of a message.
      """
      epoch = get_current_epoch(state) if message_epoch is None else message_epoch
      fork_version = state.fork.previous_version if epoch < state.fork.epoch else state.fork.current_version
      return bytes_to_int(fork_version + int_to_bytes4(domain_type))
   */
  default UInt64 get_domain(BeaconState state, UInt64 domain_type, EpochNumber message_epoch) {
    EpochNumber epoch = message_epoch == null ? get_current_epoch(state) : message_epoch;
    Bytes4 fork_version = epoch.less(state.getFork().getEpoch()) ?
        state.getFork().getPreviousVersion() : state.getFork().getCurrentVersion();
    return get_domain(fork_version, domain_type);
  }

  default UInt64 get_domain(BeaconState state, UInt64 domain_type) {
    return get_domain(state, domain_type, null);
  }

  default UInt64 get_domain(Bytes4 fork_version, UInt64 domain_type) {
    return bytes_to_int(fork_version.concat(int_to_bytes4(domain_type)));
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
    def convert_to_indexed(state: BeaconState, attestation: Attestation) -> IndexedAttestation:
      """
      Convert ``attestation`` to (almost) indexed-verifiable form.
      """
      attesting_indices = get_attesting_indices(state, attestation.data, attestation.aggregation_bitfield)
      custody_bit_1_indices = get_attesting_indices(state, attestation.data, attestation.custody_bitfield)
      custody_bit_0_indices = [index for index in attesting_indices if index not in custody_bit_1_indices]

      return IndexedAttestation(
          custody_bit_0_indices=custody_bit_0_indices,
          custody_bit_1_indices=custody_bit_1_indices,
          data=attestation.data,
          signature=attestation.signature,
      )
   */
  default IndexedAttestation convert_to_indexed(BeaconState state, Attestation attestation) {
    List<ValidatorIndex> attesting_indices =
        get_attesting_indices(state, attestation.getData(), attestation.getAggregationBitfield());
    List<ValidatorIndex> custody_bit_1_indices =
        get_attesting_indices(state, attestation.getData(), attestation.getAggregationBitfield());
    List<ValidatorIndex> custody_bit_0_indices = attesting_indices.stream()
        .filter(index -> !custody_bit_1_indices.contains(index)).collect(toList());

    return new IndexedAttestation(
        custody_bit_0_indices,
        custody_bit_1_indices,
        attestation.getData(),
        attestation.getSignature());
  }

  /*
    def verify_indexed_attestation(state: BeaconState, indexed_attestation: IndexedAttestation) -> bool:
      """
      Verify validity of ``indexed_attestation`` fields.
      """
   */
  default boolean verify_indexed_attestation(BeaconState state, IndexedAttestation indexed_attestation) {
    /*
      custody_bit_0_indices = indexed_attestation.custody_bit_0_indices
      custody_bit_1_indices = indexed_attestation.custody_bit_1_indices
     */
    ReadList<Integer, ValidatorIndex> custody_bit_0_indices = indexed_attestation.getCustodyBit0Indices();
    ReadList<Integer, ValidatorIndex> custody_bit_1_indices = indexed_attestation.getCustodyBit1Indices();

    // Ensure no duplicate indices across custody bits
    assertTrue(custody_bit_0_indices.intersection(custody_bit_1_indices).size() == 0);

    /*
      if len(custody_bit_1_indices) > 0:  # [TO BE REMOVED IN PHASE 1]
        return False
     */
    if (custody_bit_1_indices.size() > 0) {
      return false;
    }

    /*
      if not (1 <= len(custody_bit_0_indices) + len(custody_bit_1_indices) <= MAX_INDICES_PER_ATTESTATION):
        return False
     */
    int indices_in_total = custody_bit_0_indices.size() + custody_bit_1_indices.size();
    if (indices_in_total < 1
        || indices_in_total > getConstants().getMaxIndicesPerAttestation().getIntValue()) {
      return false;
    }

    /*
      if custody_bit_0_indices != sorted(custody_bit_0_indices):
        return False
      if custody_bit_1_indices != sorted(custody_bit_1_indices):
        return False
     */
    if (!Ordering.natural().isOrdered(custody_bit_0_indices)) {
      return false;
    }

    if (!Ordering.natural().isOrdered(custody_bit_1_indices)) {
      return false;
    }

    /*
      return bls_verify_multiple(
          pubkeys=[
              bls_aggregate_pubkeys([state.validator_registry[i].pubkey for i in custody_bit_0_indices]),
              bls_aggregate_pubkeys([state.validator_registry[i].pubkey for i in custody_bit_1_indices]),
          ],
          message_hashes=[
              hash_tree_root(AttestationDataAndCustodyBit(data=indexed_attestation.data, custody_bit=0b0)),
              hash_tree_root(AttestationDataAndCustodyBit(data=indexed_attestation.data, custody_bit=0b1)),
          ],
          signature=indexed_attestation.signature,
          domain=get_domain(state, DOMAIN_ATTESTATION, slot_to_epoch(indexed_attestation.data.slot)),
      )
     */
    return bls_verify_multiple(
        Arrays.asList(
            bls_aggregate_pubkeys(custody_bit_0_indices.stream()
                .map(i -> state.getValidatorRegistry().get(i).getPubKey()).collect(Collectors.toList())),
            bls_aggregate_pubkeys(custody_bit_1_indices.stream()
                .map(i -> state.getValidatorRegistry().get(i).getPubKey()).collect(Collectors.toList()))),
        Arrays.asList(
            hash_tree_root(new AttestationDataAndCustodyBit(indexed_attestation.getData(), false)),
            hash_tree_root(new AttestationDataAndCustodyBit(indexed_attestation.getData(), true))
        ),
        indexed_attestation.getSignature(),
        get_domain(state, ATTESTATION, slot_to_epoch(indexed_attestation.getData().getSlot()))
    );
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
    def get_block_root_at_slot(state: BeaconState,
                           slot: Slot) -> Bytes32:
      """
      Return the block root at a recent ``slot``.
      """
      assert slot < state.slot <= slot + SLOTS_PER_HISTORICAL_ROOT
      return state.latest_block_roots[slot % SLOTS_PER_HISTORICAL_ROOT]
  */
  default Hash32 get_block_root_at_slot(BeaconState state, SlotNumber slot) {
    assertTrue(slot.less(state.getSlot()));
    assertTrue(state.getSlot().lessEqual(slot.plus(getConstants().getSlotsPerHistoricalRoot())));
    return state.getLatestBlockRoots().get(slot.modulo(getConstants().getSlotsPerHistoricalRoot()));
  }

  /*
    def get_block_root(state: BeaconState,
                   epoch: Epoch) -> Bytes32:
      """
      Return the block root at a recent ``epoch``.
      """
      return get_block_root_at_slot(state, get_epoch_start_slot(epoch))
   */
  default Hash32 get_block_root(BeaconState state, EpochNumber epoch) {
    return get_block_root_at_slot(state, get_epoch_start_slot(epoch));
  }

  /*
    def get_attesting_indices(state: BeaconState,
                          attestation_data: AttestationData,
                          bitfield: bytes) -> List[ValidatorIndex]:
      """
      Return the sorted attesting indices corresponding to ``attestation_data`` and ``bitfield``.
      """
      crosslink_committees = get_crosslink_committees_at_slot(state, attestation_data.slot)
      crosslink_committee = [committee for committee, shard in crosslink_committees if shard == attestation_data.shard][0]
      assert verify_bitfield(bitfield, len(crosslink_committee))
      return sorted([index for i, index in enumerate(crosslink_committee) if get_bitfield_bit(bitfield, i) == 0b1])
   */
  default List<ValidatorIndex> get_attesting_indices(
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
    participants.sort(UInt64::compareTo);

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
      Return the current epoch if it's genesis epoch.
      """
      current_epoch = get_current_epoch(state)
      return (current_epoch - 1) if current_epoch > GENESIS_EPOCH else current_epoch
   */
  default EpochNumber get_previous_epoch(BeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    return current_epoch.greater(getConstants().getGenesisEpoch()) ?
        current_epoch.decrement() : current_epoch;
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

package org.ethereum.beacon.consensus.spec;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.ethereum.beacon.core.spec.SignatureDomains.BEACON_ATTESTER;

import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.BeaconCommittee;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.BytesValues;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * Helper functions.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.9.2/specs/core/0_beacon-chain.md#helper-functions">Helper
 *     functions</a> in ths spec.
 */
public interface HelperFunction extends SpecCommons, BLSFunctions {

  default Hash32 hash(BytesValue data) {
    return getHashFunction().apply(data);
  }


  default BeaconBlock get_empty_block() {
    BeaconBlockBody body =
        new BeaconBlockBody(
            BLSSignature.ZERO,
            new Eth1Data(Hash32.ZERO, UInt64.ZERO, Hash32.ZERO),
            Bytes32.ZERO,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            getConstants());
    return new BeaconBlock(getConstants().getGenesisSlot(), Hash32.ZERO, Hash32.ZERO, body);
  }

  default BeaconBlockHeader get_block_header(BeaconBlock block) {
    return new BeaconBlockHeader(
        block.getSlot(),
        block.getParentRoot(),
        Hash32.ZERO,
        hash_tree_root(block.getBody()));
  }

  /*
    def get_committee_count_at_slot(state: BeaconState, slot: Slot) -> uint64:
      """
      Return the number of committees at ``slot``.
      """
      epoch = compute_epoch_at_slot(slot)
      return max(1, min(
          MAX_COMMITTEES_PER_SLOT,
          len(get_active_validator_indices(state, epoch)) // SLOTS_PER_EPOCH // TARGET_COMMITTEE_SIZE,
      ))
   */
  default UInt64 get_committee_count_at_slot(BeaconState state, SlotNumber slot) {
    EpochNumber epoch = compute_epoch_at_slot(slot);
    return UInt64s.max(
        UInt64.valueOf(1),
        UInt64s.min(
            getConstants().getMaxCommitteesPerSlot(),
            UInt64.valueOf(get_active_validator_indices(state, epoch).size())
                .dividedBy(getConstants().getSlotsPerEpoch())
                .dividedBy(getConstants().getTargetCommitteeSize())
    ));
  }

  /**
   * This method has been superseded by {@link #get_beacon_committee(BeaconState, SlotNumber, CommitteeIndex)}.
   * However, it's still convenient for various log outputs, thus, it's been rewritten with usage
   * of its replacement.
   */
  default List<BeaconCommittee> get_crosslink_committees_at_slot(BeaconState state, SlotNumber slot) {
    List<BeaconCommittee> ret = new ArrayList<>();
    for (UInt64 i : UInt64s.iterate(UInt64.ZERO, get_committee_count_at_slot(state, slot))) {
      CommitteeIndex index = new CommitteeIndex(i);
      List<ValidatorIndex> committee = get_beacon_committee(state, slot, index);
      ret.add(new BeaconCommittee(committee, index));
    }

    return ret;
  }

  /*
    def get_beacon_proposer_index(state: BeaconState) -> ValidatorIndex:
      """
      Return the beacon proposer index at the current slot.
      """
      epoch = get_current_epoch(state)
      seed = hash(get_seed(state, epoch, DOMAIN_BEACON_PROPOSER) + int_to_bytes(state.slot, length=8))
      indices = get_active_validator_indices(state, epoch)
      return compute_proposer_index(state, indices, seed)
    */
  default ValidatorIndex get_beacon_proposer_index(BeaconState state) {
    EpochNumber epoch = get_current_epoch(state);
    Hash32 seed = hash(get_seed(state, epoch, SignatureDomains.BEACON_PROPOSER).concat(int_to_bytes8(state.getSlot().getValue())));
    List<ValidatorIndex> indices = get_active_validator_indices(state, epoch);
    return compute_proposer_index(state, indices, seed);
  }

  /*
    def compute_proposer_index(state: BeaconState, indices: Sequence[ValidatorIndex], seed: Hash) -> ValidatorIndex:
      """
      Return from ``indices`` a random index sampled by effective balance.
      """
      assert len(indices) > 0
      MAX_RANDOM_BYTE = 2**8 - 1
      i = 0
      while True:
          candidate_index = indices[compute_shuffled_index(ValidatorIndex(i % len(indices)), len(indices), seed)]
          random_byte = hash(seed + int_to_bytes(i // 32, length=8))[i % 32]
          effective_balance = state.validators[candidate_index].effective_balance
          if effective_balance * MAX_RANDOM_BYTE >= MAX_EFFECTIVE_BALANCE * random_byte:
              return ValidatorIndex(candidate_index)
          i += 1
   */
  int MAX_RANDOM_BYTE = (1 << 8) - 1;
  default ValidatorIndex compute_proposer_index(BeaconState state, List<ValidatorIndex> indices, Hash32 seed) {
    assertTrue(indices.size() > 0);
    int i = 0;
    while (true) {
      ValidatorIndex candidate_index = indices.get(compute_shuffled_index(ValidatorIndex.of(i % indices.size()), UInt64.valueOf(indices.size()), seed).getIntValue());
      int random_byte = hash(seed.concat(int_to_bytes8(i / Bytes32.SIZE)))
          .get(i % Bytes32.SIZE) & 0xFF;
      Gwei effective_balance = state.getValidators().get(candidate_index).getEffectiveBalance();
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
   def is_eligible_for_activation_queue(validator: Validator) -> bool:
     """
     Check if ``validator`` is eligible to be placed into the activation queue.
     """
     return (
         validator.activation_eligibility_epoch == FAR_FUTURE_EPOCH
         and validator.effective_balance == MAX_EFFECTIVE_BALANCE
     )
  */
  default boolean is_eligible_for_activation_queue(ValidatorRecord validator) {
    return validator.getActivationEligibilityEpoch().equals(getConstants().getFarFutureEpoch())
        && validator.getEffectiveBalance().equals(getConstants().getMaxEffectiveBalance());
  }

  /*
   def is_eligible_for_activation(state: BeaconState, validator: Validator) -> bool:
   """
   Check if ``validator`` is eligible for activation.
   """
   return (
       # Placement in queue is finalized
       validator.activation_eligibility_epoch <= state.finalized_checkpoint.epoch
       # Has not yet been activated
       and validator.activation_epoch == FAR_FUTURE_EPOCH
   )
  */
  default boolean is_eligible_for_activation(BeaconState state, ValidatorRecord validator) {
    return
      // Placement in queue is finalized
      validator.getActivationEligibilityEpoch().lessEqual(state.getFinalizedCheckpoint().getEpoch())
      // Has not yet been activated
      && validator.getActivationEpoch().equals(getConstants().getFarFutureEpoch());
  }

  /*
    def get_active_validator_indices(state: BeaconState, epoch: Epoch) -> Sequence[ValidatorIndex]:
      """
      Return the sequence of active validator indices at ``epoch``.
      """
      return [ValidatorIndex(i) for i, v in enumerate(state.validators) if is_active_validator(v, epoch)]
    */
  default List<ValidatorIndex> get_active_validator_indices(BeaconState state, EpochNumber epoch) {
    ArrayList<ValidatorIndex> ret = new ArrayList<>();
    for (ValidatorIndex i : state.getValidators().size()) {
      if (is_active_validator(state.getValidators().get(i), epoch)) {
        ret.add(i);
      }
    }
    return ret;
  }

  /** {@link #get_active_validator_indices(BeaconState, EpochNumber)} wrapped with limited list */
  default ReadList<Integer, ValidatorIndex> get_active_validator_indices_list(
      BeaconState state, EpochNumber epoch) {
    List<ValidatorIndex> indices = new ArrayList<>(get_active_validator_indices(state, epoch));
    return ReadList.wrap(
        indices, Integer::new, getConstants().getValidatorRegistryLimit().longValue());
  }

  /*
    def increase_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei) -> None:
      """
      Increase the validator balance at index ``index`` by ``delta``.
      """
      state.balances[index] += delta
   */
  default void increase_balance(MutableBeaconState state, ValidatorIndex index, Gwei delta) {
    state.getBalances().update(index, balance -> balance.plus(delta));
  }

  /*
    def decrease_balance(state: BeaconState, index: ValidatorIndex, delta: Gwei) -> None:
      """
      Decrease the validator balance at index ``index`` by ``delta``, with underflow protection.
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
    def get_randao_mix(state: BeaconState, epoch: Epoch) -> Hash:
      """
      Return the randao mix at a recent ``epoch``.
      """
      return state.randao_mixes[epoch % EPOCHS_PER_HISTORICAL_VECTOR]
    */
  default Hash32 get_randao_mix(BeaconState state, EpochNumber epoch) {
    return state.getRandaoMixes().get(
        epoch.modulo(getConstants().getEpochsPerHistoricalVector()));
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

  default BytesValue int_to_bytes1(UInt64 value) {
    return int_to_bytes1(value.getIntValue());
  }

  default Bytes4 int_to_bytes4(long value) {
    return Bytes4.ofUnsignedIntLittleEndian(value & 0xFFFFFF);
  }

  default Bytes8 int_to_bytes8(long value) {
    return Bytes8.longToBytes8LittleEndian(value);
  }

  default Bytes8 int_to_bytes8(UInt64 value) {
    return Bytes8.longToBytes8LittleEndian(value.getValue());
  }

  default Bytes4 int_to_bytes4(UInt64 value) {
    return int_to_bytes4(value.getValue());
  }

  default Bytes32 int_to_bytes32(UInt64 value) {
    return Bytes32.rightPad(value.toBytes8LittleEndian());
  }

  /*
    def compute_shuffled_index(index: ValidatorIndex, index_count: int, seed: Bytes32) -> ValidatorIndex:
      """
      Return the shuffled validator index corresponding to ``seed`` (and ``index_count``).
      """
      assert index < index_count

      # Swap or not (https://link.springer.com/content/pdf/10.1007%2F978-3-642-32009-5_1.pdf)
      # See the 'generalized domain' algorithm on page 3
      for round in range(SHUFFLE_ROUND_COUNT):
          pivot = bytes_to_int(hash(seed + int_to_bytes1(round))[0:8]) % index_count
          flip = (pivot - index) % index_count
          position = max(index, flip)
          source = hash(seed + int_to_bytes1(round) + int_to_bytes4(position // 256))
          byte = source[(position % 256) // 8]
          bit = (byte >> (position % 8)) % 2
          index = flip if bit else index

      return index
   */
  default UInt64 compute_shuffled_index(UInt64 index, UInt64 index_count, Bytes32 seed) {
    assertTrue(index.compareTo(index_count) < 0);

    for (int round = 0; round < getConstants().getShuffleRoundCount(); round++) {
      Bytes8 pivotBytes = Bytes8.wrap(hash(seed.concat(int_to_bytes1(round))), 0);
      long pivot = bytes_to_int(pivotBytes).modulo(index_count).getValue();
      UInt64 flip = UInt64.valueOf(Math.floorMod(pivot + index_count.getValue() - index.getValue(),
          index_count.getValue()));
      UInt64 position = UInt64s.max(index, flip);
      Bytes4 positionBytes = int_to_bytes4(position.dividedBy(UInt64.valueOf(256)));
      Bytes32 source = hash(seed.concat(int_to_bytes1(round)).concat(positionBytes));
      int byteV = source.get(position.modulo(256).getIntValue() / 8) & 0xFF;
      int bit = ((byteV >> (position.modulo(8).getIntValue())) % 2) & 0xFF;
      index = bit > 0 ? flip : index;
    }

    return index;
  }

  /*
    def compute_committee(indices: List[ValidatorIndex], seed: Bytes32, index: int, count: int) -> List[ValidatorIndex]:
      start = (len(indices) * index) // count
      end = (len(indices) * (index + 1)) // count
      return [indices[compute_shuffled_index(i, len(indices), seed)] for i in range(start, end)]
   */
  default List<ValidatorIndex> compute_committee(List<ValidatorIndex> indices, Bytes32 seed, UInt64 index, UInt64 count) {
    UInt64 start = index.times(indices.size()).dividedBy(count);
    UInt64 end = index.increment().times(indices.size()).dividedBy(count);

    return compute_committee(indices, start, end, seed);
  }

  default List<ValidatorIndex> compute_committee(List<ValidatorIndex> validator_indices, UInt64 start, UInt64 end, Bytes32 seed) {
    List<ValidatorIndex> result = new ArrayList<>();
    for (UInt64 i = start; i.compareTo(end) < 0; i = i.increment()) {
      UInt64 shuffled_index = compute_shuffled_index(i, UInt64.valueOf(validator_indices.size()), seed);
      result.add(validator_indices.get(shuffled_index.getIntValue()));
    }
    return result;
  }

  /**
   * An optimized version of {@link #compute_committee(List, Bytes32, UInt64, UInt64)}.
   * Based on {@link #get_permuted_list(List, Bytes32)}.
   */
  default List<ValidatorIndex> compute_committee2(List<ValidatorIndex> indices, Bytes32 seed, UInt64 index, UInt64 count) {
    UInt64 start = index.times(indices.size()).dividedBy(count);
    UInt64 end = index.increment().times(indices.size()).dividedBy(count);
    return compute_committee2(indices, start, end, seed);
  }

  default List<ValidatorIndex> compute_committee2(List<ValidatorIndex> validator_indices, UInt64 start, UInt64 end, Bytes32 seed) {
    List<ValidatorIndex> shuffled_indices = get_permuted_list(validator_indices, seed)
        .stream().map(ValidatorIndex::new).collect(toList());
    return shuffled_indices.subList(start.intValue(), end.intValue());
  }

  /*
    def get_beacon_committee(state: BeaconState, slot: Slot, index: CommitteeIndex) -> Sequence[ValidatorIndex]:
      """
      Return the beacon committee at ``slot`` for ``index``.
      """
      epoch = compute_epoch_at_slot(slot)
      committees_per_slot = get_committee_count_at_slot(state, slot)
      return compute_committee(
          indices=get_active_validator_indices(state, epoch),
          seed=get_seed(state, epoch, DOMAIN_BEACON_ATTESTER),
          index=(slot % SLOTS_PER_EPOCH) * committees_per_slot + index,
          count=committees_per_slot * SLOTS_PER_EPOCH,
      )
   */
  default List<ValidatorIndex> get_beacon_committee(BeaconState state, SlotNumber slot, CommitteeIndex index) {
    EpochNumber epoch = compute_epoch_at_slot(slot);
    UInt64 committees_per_slot = get_committee_count_at_slot(state, slot);
    return compute_committee2(
        get_active_validator_indices(state, epoch),
        get_seed(state, epoch, BEACON_ATTESTER),
        slot.modulo(getConstants().getSlotsPerEpoch()).times(committees_per_slot).plus(index),
        committees_per_slot.times(getConstants().getSlotsPerEpoch())
    );
  }

  /*
   def is_valid_merkle_branch(leaf: Bytes32, proof: List[Bytes32], depth: int, index: int, root: Bytes32) -> bool:
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
  default boolean is_valid_merkle_branch(
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
    def get_total_balance(state: BeaconState, indices: Set[ValidatorIndex]) -> Gwei:
      """
      Return the combined effective balance of the ``indices``. (1 Gwei minimum to avoid divisions by zero.)
      """
      return Gwei(max(sum([state.validators[index].effective_balance for index in indices]), 1))
   */
  default Gwei get_total_balance(BeaconState state, Collection<ValidatorIndex> indices) {
    return UInt64s.max(indices.stream()
        .map(index -> state.getValidators().get(index).getEffectiveBalance())
        .reduce(Gwei.ZERO, Gwei::plus), Gwei.of(1));
  }

  /*
    def get_total_active_balance(state: BeaconState) -> Gwei:
      """
      Return the combined effective balance of the active validators.
      """
      return get_total_balance(state, set(get_active_validator_indices(state, get_current_epoch(state))))
   */
  default Gwei get_total_active_balance(BeaconState state) {
    return get_total_balance(state,
        get_active_validator_indices(state, get_current_epoch(state)));
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
    def compute_activation_exit_epoch(epoch: Epoch) -> Epoch:
      """
      Return the epoch during which validator activations and exits initiated in ``epoch`` take effect.
      """
      return Epoch(epoch + 1 + MAX_SEED_LOOKAHEAD)
   */
  default EpochNumber compute_activation_exit_epoch(EpochNumber epoch) {
    return epoch.plus(1).plus(getConstants().getMaxSeedLookahead());
  }

  /*
    def get_validator_churn_limit(state: BeaconState) -> uint64:
      """
      Return the validator churn limit for the current epoch.
      """
      active_validator_indices = get_active_validator_indices(state, get_current_epoch(state))
      return max(MIN_PER_EPOCH_CHURN_LIMIT, len(active_validator_indices) // CHURN_LIMIT_QUOTIENT)
   */
  default UInt64 get_validator_churn_limit(BeaconState state) {
    List<ValidatorIndex> active_validator_indices =
        get_active_validator_indices(state, get_current_epoch(state));
    return UInt64s.max(
        getConstants().getMinPerEpochChurnLimit(),
        UInt64.valueOf(active_validator_indices.size())
            .dividedBy(getConstants().getChurnLimitQuotient())
    );
  }

  /*
    def slash_validator(state: BeaconState,
                    slashed_index: ValidatorIndex,
                    whistleblower_index: ValidatorIndex=None) -> None:
      """
      Slash the validator with index ``slashed_index``.
      """
      epoch = get_current_epoch(state)
      initiate_validator_exit(state, slashed_index)
      validator = state.validators[slashed_index]
      validator.slashed = True
      validator.withdrawable_epoch = max(validator.withdrawable_epoch, Epoch(epoch + EPOCHS_PER_SLASHINGS_VECTOR))
      state.slashings[epoch % EPOCHS_PER_SLASHINGS_VECTOR] += validator.effective_balance
      decrease_balance(state, slashed_index, validator.effective_balance // MIN_SLASHING_PENALTY_QUOTIENT)

      # Apply proposer and whistleblower rewards
      proposer_index = get_beacon_proposer_index(state)
      if whistleblower_index is None:
          whistleblower_index = proposer_index
      whistleblower_reward = Gwei(validator.effective_balance // WHISTLEBLOWER_REWARD_QUOTIENT)
      proposer_reward = Gwei(whistleblower_reward // PROPOSER_REWARD_QUOTIENT)
      increase_balance(state, proposer_index, proposer_reward)
      increase_balance(state, whistleblower_index, whistleblower_reward - proposer_reward)
    */
  default void slash_validator(MutableBeaconState state, ValidatorIndex slashed_index, ValidatorIndex whistleblower_index) {
    EpochNumber epoch = get_current_epoch(state);
    initiate_validator_exit(state, slashed_index);
    state
        .getValidators()
        .update(
            slashed_index,
            validator ->
                ValidatorRecord.Builder.fromRecord(validator)
                    .withSlashed(Boolean.TRUE)
                    .withWithdrawableEpoch(
                        EpochNumber.castFrom(
                            UInt64s.max(
                                validator.getWithdrawableEpoch(),
                                epoch.plus(getConstants().getEpochsPerSlashingsVector()))))
                    .build());
    Gwei slashed_balance = state.getValidators().get(slashed_index).getEffectiveBalance();
    state.getSlashings().update(epoch.modulo(getConstants().getEpochsPerSlashingsVector()),
        balance -> balance.plus(slashed_balance));
    decrease_balance(state, slashed_index, state.getValidators().get(slashed_index)
        .getEffectiveBalance().dividedBy(getConstants().getMinSlashingPenaltyQuotient()));

    ValidatorIndex proposer_index = get_beacon_proposer_index(state);
    if (whistleblower_index == null) {
      whistleblower_index = proposer_index;
    }
    Gwei whistleblowing_reward = slashed_balance.dividedBy(getConstants().getWhistleblowerRewardQuotient());
    Gwei proposer_reward = whistleblowing_reward.dividedBy(getConstants().getProposerRewardQuotient());
    increase_balance(state, proposer_index, proposer_reward);
    increase_balance(state, whistleblower_index, whistleblowing_reward.minus(proposer_reward));
  }

  default void slash_validator(MutableBeaconState state, ValidatorIndex slashed_index) {
    slash_validator(state, slashed_index, null);
  }

  /*
    def initiate_validator_exit(state: BeaconState, index: ValidatorIndex) -> None:
      """
      Initiate the exit of the validator with index ``index``.
      """

      # Compute exit queue epoch
      exit_epochs = [v.exit_epoch for v in state.validators if v.exit_epoch != FAR_FUTURE_EPOCH]
      exit_queue_epoch = max(exit_epochs + [compute_activation_exit_epoch(get_current_epoch(state))])
      exit_queue_churn = len([v for v in state.validators if v.exit_epoch == exit_queue_epoch])
      if exit_queue_churn >= get_validator_churn_limit(state):
          exit_queue_epoch += Epoch(1)

      # Set validator exit epoch and withdrawable epoch
      validator.exit_epoch = exit_queue_epoch
      validator.withdrawable_epoch = Epoch(validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY)
  */
  default void initiate_validator_exit(MutableBeaconState state, ValidatorIndex index) {
    /* # Return if validator already initiated exit
      validator = state.validators[index]
      if validator.exit_epoch != FAR_FUTURE_EPOCH:
          return */
    checkIndexRange(state, index);
    if (!state.getValidators().get(index).getExitEpoch().equals(getConstants().getFarFutureEpoch())) {
      return;
    }

    /* # Compute exit queue epoch
      exit_epochs = [v.exit_epoch for v in state.validators if v.exit_epoch != FAR_FUTURE_EPOCH]
      exit_queue_epoch = max(exit_epochs + [compute_activation_exit_epoch(get_current_epoch(state))])
      exit_queue_churn = len([v for v in state.validators if v.exit_epoch == exit_queue_epoch])
      if exit_queue_churn >= get_validator_churn_limit(state):
          exit_queue_epoch += Epoch(1) */
    EpochNumber exit_queue_epoch = Stream.concat(
        state.getValidators().stream()
            .filter(v -> !v.getExitEpoch().equals(getConstants().getFarFutureEpoch()))
            .map(ValidatorRecord::getExitEpoch),
        Stream.of(compute_activation_exit_epoch(get_current_epoch(state)))
    ).max(EpochNumber::compareTo).get();

    long exit_queue_churn = 0;
    for (ValidatorRecord validatorRecord : state.getValidators()) {
      if (validatorRecord.getExitEpoch().equals(exit_queue_epoch)) {
        ++exit_queue_churn;
      }
    }
    if (UInt64.valueOf(exit_queue_churn).compareTo(get_validator_churn_limit(state)) >= 0) {
      exit_queue_epoch = exit_queue_epoch.increment();
    }

    /* # Set validator exit epoch and withdrawable epoch
      validator.exit_epoch = exit_queue_epoch
      validator.withdrawable_epoch = Epoch(validator.exit_epoch + MIN_VALIDATOR_WITHDRAWABILITY_DELAY) */
    final EpochNumber exitEpoch = exit_queue_epoch;
    state.getValidators().update(index,
        validator -> ValidatorRecord.Builder.fromRecord(validator)
            .withExitEpoch(exitEpoch)
            .withWithdrawableEpoch(exitEpoch.plus(HelperFunction.this.getConstants().getMinValidatorWithdrawabilityDelay()))
            .build());
  }

  /** Function for hashing objects into a single root utilizing a hash tree structure */
  default Hash32 hash_tree_root(Object object) {
    return getObjectHasher().getHash(object);
  }

  /*
    def get_seed(state: BeaconState, epoch: Epoch, domain_type: DomainType) -> Hash:
      """
      Return the seed at ``epoch``.
      """
      mix = get_randao_mix(state, Epoch(epoch + EPOCHS_PER_HISTORICAL_VECTOR - MIN_SEED_LOOKAHEAD - 1))  # Avoid underflow
      return hash(domain_type + int_to_bytes(epoch, length=8) + mix)
   */
  default Hash32 get_seed(BeaconState state, EpochNumber epoch, UInt64 domain_type) {
    Hash32 mix = get_randao_mix(state,
        epoch.plus(getConstants().getEpochsPerHistoricalVector()
            .minus(getConstants().getMinSeedLookahead())
            .decrement()));
    return hash(int_to_bytes4(domain_type).concat(int_to_bytes8(epoch)).concat(mix));
  }

  /*
    def get_domain(state: BeaconState, domain_type: DomainType, message_epoch: Epoch=None) -> Domain:
      """
      Return the signature domain (fork version concatenated with domain type) of a message.
      """
      epoch = get_current_epoch(state) if message_epoch is None else message_epoch
      fork_version = state.fork.previous_version if epoch < state.fork.epoch else state.fork.current_version
      return compute_domain(domain_type, fork_version)
   */
  default UInt64 get_domain(BeaconState state, UInt64 domain_type, EpochNumber message_epoch) {
    EpochNumber epoch = message_epoch == null ? get_current_epoch(state) : message_epoch;
    Bytes4 fork_version = epoch.less(state.getFork().getEpoch()) ?
        state.getFork().getPreviousVersion() : state.getFork().getCurrentVersion();
    return compute_domain(domain_type, fork_version);
  }

  default UInt64 get_domain(BeaconState state, UInt64 domain_type) {
    return get_domain(state, domain_type, null);
  }

  /*
    def is_slashable_attestation_data(data_1: AttestationData, data_2: AttestationData) -> bool:
      """
      Check if ``data_1`` and ``data_2`` are slashable according to Casper FFG rules.
      """
      return (
          # Double vote
          (data_1 != data_2 and data_1.target.epoch == data_2.target.epoch) or
          # Surround vote
          (data_1.source.epoch < data_2.source.epoch and data_2.target.epoch < data_1.target.epoch)
      )
   */
  default boolean is_slashable_attestation_data(AttestationData data_1, AttestationData data_2) {
    return
        // Double vote
        (!data_1.equals(data_2) && data_1.getTarget().getEpoch().equals(data_2.getTarget().getEpoch()))
        // Surround vote
        || (data_1.getSource().getEpoch().less(data_2.getSource().getEpoch())
            && data_2.getTarget().getEpoch().less(data_1.getTarget().getEpoch()));
  }

  default List<BLSPubkey> mapIndicesToPubKeys(BeaconState state, Iterable<ValidatorIndex> indices) {
    List<BLSPubkey> publicKeys = new ArrayList<>();
    for (ValidatorIndex index : indices) {
      checkIndexRange(state, index);
      publicKeys.add(state.getValidators().get(index).getPubKey());
    }
    return publicKeys;
  }

  /*
    def get_indexed_attestation(state: BeaconState, attestation: Attestation) -> IndexedAttestation:
      """
      Return the indexed attestation corresponding to ``attestation``.
      """
      attesting_indices = get_attesting_indices(state, attestation.data, attestation.aggregation_bits)

      return IndexedAttestation(
          attesting_indices=sorted(attesting_indices),
          data=attestation.data,
          signature=attestation.signature,
      )
   */
  default IndexedAttestation get_indexed_attestation(BeaconState state, Attestation attestation) {
    List<ValidatorIndex> attesting_indices =
        get_attesting_indices(state, attestation.getData(), attestation.getAggregationBits());

    Collections.sort(attesting_indices);

    return new IndexedAttestation(
        attesting_indices,
        attestation.getData(),
        attestation.getSignature(),
        getConstants());
  }

  /*
    def is_valid_indexed_attestation(state: BeaconState, indexed_attestation: IndexedAttestation) -> None:
      """
      Verify validity of ``indexed_attestation``.
      """
   */
  default boolean is_valid_indexed_attestation(BeaconState state, IndexedAttestation indexed_attestation) {
    /*
      indices = indexed_attestation.attesting_indices
     */
    ReadList<Integer, ValidatorIndex> indices = indexed_attestation.getAttestingIndices();

    if (indices.size() > getConstants().getMaxValidatorsPerCommittee().getIntValue()) {
      return false;
    }

    // Verify indices are sorted and unique
    if (!Ordering.natural().isOrdered(indices)) {
      return false;
    }

    if (indices.stream().distinct().count() < indices.size()) {
      return false;
    }

    /*
     return bls_verify(
         pubkey=bls_aggregate_pubkeys([state.validators[i].pubkey for i in indices]),
         message_hash=hash_tree_root(indexed_attestation.data),
         signature=indexed_attestation.signature,
         domain=get_domain(state, DOMAIN_ATTESTATION, indexed_attestation.data.target.epoch),
     )
    */
    return bls_verify(
        BLSPubkey.wrap(
            bls_aggregate_pubkeys(
                    indices.stream()
                        .map(i -> state.getValidators().get(i).getPubKey())
                        .collect(Collectors.toList()))
                .getEncodedBytes()),
        hash_tree_root(indexed_attestation.getData()),
        indexed_attestation.getSignature(),
        get_domain(state, BEACON_ATTESTER, indexed_attestation.getData().getTarget().getEpoch()));
  }

  /*
    def get_block_root_at_slot(state: BeaconState, slot: Slot) -> Hash:
      """
      Return the block root at a recent ``slot``.
      """
      assert slot < state.slot <= slot + SLOTS_PER_HISTORICAL_ROOT
      return state.block_roots[slot % SLOTS_PER_HISTORICAL_ROOT]
  */
  default Hash32 get_block_root_at_slot(BeaconState state, SlotNumber slot) {
    assertTrue(slot.less(state.getSlot()));
    assertTrue(state.getSlot().lessEqual(slot.plus(getConstants().getSlotsPerHistoricalRoot())));
    return state.getBlockRoots().get(slot.modulo(getConstants().getSlotsPerHistoricalRoot()));
  }

  /*
    def get_block_root(state: BeaconState, epoch: Epoch) -> Hash:
      """
      Return the block root at the start of a recent ``epoch``.
      """
      return get_block_root_at_slot(state, compute_start_slot_of_epoch(epoch))
   */
  default Hash32 get_block_root(BeaconState state, EpochNumber epoch) {
    return get_block_root_at_slot(state, compute_start_slot_at_epoch(epoch));
  }

  /*
    def get_attesting_indices(state: BeaconState,
                          data: AttestationData,
                          bits: Bitlist[MAX_VALIDATORS_PER_COMMITTEE]) -> Set[ValidatorIndex]:
      """
      Return the set of attesting indices corresponding to ``data`` and ``bits``.
      """
      committee = get_beacon_committee(state, data.slot, data.index)
      return set(index for i, index in enumerate(committee) if bits[i])
   */
  default List<ValidatorIndex> get_attesting_indices(
      BeaconState state, AttestationData attestation_data, Bitlist bitList) {
    List<ValidatorIndex> committee =
        get_beacon_committee(state, attestation_data.getSlot(),
            attestation_data.getIndex());
    List<ValidatorIndex> participants = new ArrayList<>();
    for (int i = 0; i < committee.size(); i++) {
      ValidatorIndex validator_index = committee.get(i);
      boolean aggregation_bit = bitList.getBit(i);
      if (aggregation_bit) {
        participants.add(validator_index);
      }
    }

    return participants;
  }

  default ValidatorIndex get_validator_index_by_pubkey(BeaconState state, BLSPubkey pubkey) {
    ValidatorIndex index = ValidatorIndex.MAX;
    for (ValidatorIndex i : state.getValidators().size()) {
      if (state.getValidators().get(i).getPubKey().equals(pubkey)) {
        index = i;
        break;
      }
    }

    return index;
  }

  /*
    def compute_domain(domain_type: DomainType, fork_version: Version=Version()) -> Domain:
      """
      Return the domain for the ``domain_type`` and ``fork_version``.
      """
      return Domain(domain_type + fork_version)
   */
  default UInt64 compute_domain(UInt64 domain_type, Bytes4 fork_version) {
    return bytes_to_int(int_to_bytes4(domain_type).concat(fork_version));
  }

  default UInt64 compute_domain(UInt64 domain_type) {
    return compute_domain(domain_type, Bytes4.ZERO);
  }

  /*
   def compute_epoch_at_slot(slot: Slot) -> Epoch:
      """
      Return the epoch number of ``slot``.
      """
      return Epoch(slot // SLOTS_PER_EPOCH)
  */
  default EpochNumber compute_epoch_at_slot(SlotNumber slot) {
    return slot.dividedBy(getConstants().getSlotsPerEpoch());
  }

  /*
    def get_previous_epoch(state: BeaconState) -> Epoch:
      """`
      Return the previous epoch (unless the current epoch is ``GENESIS_EPOCH``).
      """
      current_epoch = get_current_epoch(state)
      return GENESIS_EPOCH if current_epoch == GENESIS_EPOCH else Epoch(current_epoch - 1)
   */
  default EpochNumber get_previous_epoch(BeaconState state) {
    EpochNumber current_epoch = get_current_epoch(state);
    return current_epoch.equals(getConstants().getGenesisEpoch()) ?
        getConstants().getGenesisEpoch() : current_epoch.decrement();
  }

  /*
   def get_current_epoch(state: BeaconState) -> Epoch:
      """
      Return the current epoch.
      """
      return compute_epoch_of_slot(state.slot)
  */
  default EpochNumber get_current_epoch(BeaconState state) {
    return compute_epoch_at_slot(state.getSlot());
  }

  /*
    def compute_start_slot_of_epoch(epoch: Epoch) -> Slot:
      """
      Return the start slot of ``epoch``.
      """
      return Slot(epoch * SLOTS_PER_EPOCH)
   */
  default SlotNumber compute_start_slot_at_epoch(EpochNumber epoch) {
    return epoch.mul(getConstants().getSlotsPerEpoch());
  }

  /*
    def compute_slots_since_epoch_start(slot: Slot) -> int:
      return slot - compute_start_slot_at_epoch(compute_epoch_at_slot(slot))
   */
  default SlotNumber compute_slots_since_epoch_start(SlotNumber slot) {
    return slot.minus(compute_start_slot_at_epoch(compute_epoch_at_slot(slot)));
  }
}

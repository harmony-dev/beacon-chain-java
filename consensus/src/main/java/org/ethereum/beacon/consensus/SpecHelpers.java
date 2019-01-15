package org.ethereum.beacon.consensus;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.Hashes;
import org.javatuples.Pair;
import org.ethereum.beacon.crypto.MessageParameters;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes3;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
  public List<Pair<UInt24[], UInt64>> get_shard_committees_at_slot(BeaconState state, UInt64 slot) {
    UInt64 state_epoch_slot = state.getSlot().minus(state.getSlot().modulo(spec.getEpochLength()));
    assertTrue(state_epoch_slot.compareTo(slot.plus(spec.getEpochLength())) <= 0);
    assertTrue(slot.compareTo(state_epoch_slot.plus(spec.getEpochLength())) < 0);

    //    offset = slot % EPOCH_LENGTH
    UInt64 offset = slot.modulo(spec.getEpochLength());

    //    if slot < state_epoch_slot:
    int committees_per_slot;
    int[][] shuffling;
    UInt64 slot_start_shard;
    if (slot.compareTo(state_epoch_slot) < 0) {
      //      committees_per_slot = get_previous_epoch_committees_per_slot(state)
      committees_per_slot = get_previous_epoch_committees_per_slot(state);
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
      committees_per_slot = get_current_epoch_committees_per_slot(state);
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
      int[] shuffling1 = shuffling[offset.times(committees_per_slot).plus(i).getIntValue()];
      UInt24[] shuffling2 = new UInt24[shuffling1.length];
      for (int i1 = 0; i1 < shuffling1.length; i1++) {
        shuffling2[i1] = UInt24.valueOf(shuffling1[i1]);
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

  //  def get_shuffling(randao_mix: Hash32,
  //              validators: List[ValidatorRecord],
  //              slot: int) -> List[List[int]]
  //  """
  //  Shuffles ``validators`` into shard committees seeded by ``seed`` and ``slot``.
  //  Returns a list of ``EPOCH_LENGTH * committees_per_slot`` committees where each
  //  committee is itself a list of validator indices.
  //  """
  public int[][] get_shuffling(Hash32 _seed,
                               ValidatorRecord[] validators,
                               UInt64 _slot) {


    //
    //      # Normalizes slot to start of epoch boundary
    //  slot -= slot % EPOCH_LENGTH
    UInt64 slot = _slot.minus(_slot.modulo(spec.getEpochLength()));
    //      active_validator_indices = get_active_validator_indices(validators, slot)
    int[] active_validator_indices = get_active_validator_indices(validators, slot);
    //      committees_per_slot = get_committee_count_per_slot(len(active_validator_indices))
    int committees_per_slot = get_committee_count_per_slot(active_validator_indices.length);

    //      # Shuffle
    //      seed = xor(seed, bytes32(slot))
    Hash32 seed = Hash32.wrap(Bytes32s.xor(_seed, Bytes32.leftPad(slot.toBytesBigEndian())));

    //  shuffled_active_validator_indices = shuffle(active_validator_indices, seed)
    int[] shuffled_active_validator_indices = shuffle(active_validator_indices, seed);
    //    # Split the shuffled list into epoch_length * committees_per_slot pieces
    //    return split(shuffled_active_validator_indices, committees_per_slot * EPOCH_LENGTH)
    return split(shuffled_active_validator_indices,
        spec.getEpochLength()
            .times(committees_per_slot)
            .getIntValue());
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

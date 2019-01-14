package org.ethereum.beacon.consensus;

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
import org.ethereum.beacon.crypto.MessageParameters;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes3;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

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
    return Hashes.keccack256(data);
  }

  /*
   state_epoch_slot = state.slot - (state.slot % EPOCH_LENGTH)
   assert state_epoch_slot <= slot + EPOCH_LENGTH
   assert slot < state_epoch_slot + EPOCH_LENGTH
   return state.shard_committees_at_slots[slot - state_epoch_slot + EPOCH_LENGTH]
  */
  public ShardCommittee[] get_shard_committees_at_slot(BeaconState state, UInt64 slot) {
    UInt64 state_epoch_slot = state.getSlot().minus(state.getSlot().modulo(spec.getEpochLength()));
    assertTrue(state_epoch_slot.compareTo(slot.plus(spec.getEpochLength())) <= 0);
    assertTrue(slot.compareTo(state_epoch_slot.plus(spec.getEpochLength())) < 0);
    return state
        .getShardCommitteesAtSlotsUnsafe()[
        safeInt(slot.minus(state_epoch_slot).plus(spec.getEpochLength()))];
  }

  /*
   first_committee = get_shard_committees_at_slot(state, slot)[0].committee
   return first_committee[slot % len(first_committee)]
  */
  public UInt24 get_beacon_proposer_index(BeaconState state, UInt64 slot) {
    ShardCommittee[] committees = get_shard_committees_at_slot(state, slot);
    UInt24[] first_committee = committees[0].getCommittee();
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
          //    output[index], output[replacement_position] = output[replacement_position],
          // output[index]
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
  public ShardCommittee[][] get_shuffling(
      Hash32 seed, ValidatorRecord[] validators, int crosslinking_start_shard, UInt64 _slot) {

    //     # Normalizes slot to start of epoch boundary
    //    slot -= slot % EPOCH_LENGTH
    UInt64 slot = _slot.minus(_slot.modulo(spec.getEpochLength()));

    //  active_validator_indices = get_active_validator_indices(validators, slot)
    int[] active_validator_indices = get_active_validator_indices(validators, slot);

    return get_active_shuffling(seed, active_validator_indices, crosslinking_start_shard);
  }

  ShardCommittee[][] get_active_shuffling(
      Hash32 seed, int[] active_validator_indices, int crosslinking_start_shard) {

    //    committees_per_slot = max(
    //        1,
    //        min(
    //            SHARD_COUNT // EPOCH_LENGTH,
    //            len(active_validator_indices) // EPOCH_LENGTH // TARGET_COMMITTEE_SIZE,
    //        )
    //    )
    int committees_per_slot =
        max(
            1,
            min(
                spec.getShardCount().dividedBy(spec.getEpochLength()).getIntValue(),
                UInt64.valueOf(active_validator_indices.length)
                    .dividedBy(spec.getEpochLength())
                    .dividedBy(spec.getTargetCommitteeSize().getValue())
                    .getIntValue()));

    //    # Shuffle
    // TODO    seed = xor(randao_mix, bytes32(slot)) - looks obsolete
    //    shuffled_active_validator_indices = shuffle(active_validator_indices, seed)
    int[] shuffled_active_validator_indices = shuffle(active_validator_indices, seed);

    //    # Split the shuffled list into epoch_length pieces
    //    validators_per_slot = split(shuffled_active_validator_indices, EPOCH_LENGTH)
    int[][] validators_per_slot =
        split(shuffled_active_validator_indices, spec.getEpochLength().getIntValue());

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
        shard_committees[shard_position] =
            new ShardCommittee(
                UInt64.valueOf(shard_id_start + shard_position),
                Arrays.stream(indices).mapToObj(UInt24::valueOf).toArray(UInt24[]::new),
                UInt64.valueOf(active_validator_indices.length));
      }

      //      output.append(shard_committees)
      output.add(shard_committees);
    }

    return output.toArray(new ShardCommittee[0][]);
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
    assert publicKeys.length == messages.length;

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

    List<Bytes48> pubKeys1 =
        Stream.of(vote_data.getCustodyBit0Indices())
            .map(index -> state.getValidatorRegistryUnsafe().get(safeInt(index)).getPubKey())
            .collect(Collectors.toList());
    List<Bytes48> pubKeys2 =
        Stream.of(vote_data.getCustodyBit1Indices())
            .map(index -> state.getValidatorRegistryUnsafe().get(safeInt(index)).getPubKey())
            .collect(Collectors.toList());

    return bls_verify_multiple(
        new PublicKey[] {bls_aggregate_pubkeys(pubKeys1), bls_aggregate_pubkeys(pubKeys2)},
        new Hash32[] {
          hash_tree_root(new AttestationDataAndCustodyBit(vote_data.getData(), false)),
          hash_tree_root(new AttestationDataAndCustodyBit(vote_data.getData(), true))
        },
        vote_data.getAggregatedSignature(),
        get_domain(state.getForkData(), state.getSlot(), ATTESTATION));
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

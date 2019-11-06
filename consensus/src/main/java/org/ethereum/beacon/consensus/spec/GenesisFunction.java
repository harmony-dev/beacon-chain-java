package org.ethereum.beacon.consensus.spec;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * On genesis part.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.8.1/specs/core/0_beacon-chain.md#genesis">Genesis</a>
 *     in the spec.
 */
public interface GenesisFunction extends BlockProcessing {

  /*
   Before the Ethereum 2.0 genesis has been triggered, and for every Ethereum 1.0 block,
   let candidate_state = initialize_beacon_state_from_eth1(eth1_block_hash, eth1_timestamp, deposits) where:

    - eth1_block_hash is the hash of the Ethereum 1.0 block
    - eth1_timestamp is the Unix timestamp corresponding to eth1_block_hash
    - deposits is the sequence of all deposits, ordered chronologically, up to the block with hash eth1_block_hash
  */
  default BeaconState initialize_beacon_state_from_eth1(
      Hash32 eth1_block_hash, Time eth1_timestamp, List<Deposit> deposits) {
    MutableBeaconState state = BeaconState.getEmpty(getConstants()).createMutableCopy();

    state.setPreviousJustifiedCheckpoint(
        new Checkpoint(getConstants().getGenesisEpoch(), Hash32.ZERO));
    state.setCurrentJustifiedCheckpoint(
        new Checkpoint(getConstants().getGenesisEpoch(), Hash32.ZERO));
    state.setFinalizedCheckpoint(
        new Checkpoint(getConstants().getGenesisEpoch(), Hash32.ZERO));

    state.setSlot(getConstants().getGenesisSlot());
    state.setGenesisTime(compute_genesis_time(eth1_timestamp));
    state.setFork(new Fork(Bytes4.ZERO, Bytes4.ZERO, getConstants().getGenesisEpoch()));
    state.setLatestBlockHeader(get_block_header(get_empty_block()));
    // randao_mixes=[eth1_block_hash] * EPOCHS_PER_HISTORICAL_VECTOR,  # Seed RANDAO with Eth1 entropy
    List<Hash32> randao_mixes = IntStream.range(0, getConstants().getEpochsPerHistoricalVector().intValue()).mapToObj(i -> eth1_block_hash).collect(Collectors.toList());
    state.setRandaoMixes(WriteList.wrap(randao_mixes, EpochNumber::new, true));

    // Process deposits
    ReadList<Integer, DepositData> deposit_data_list =
        ReadList.wrap(
            deposits.stream().map(Deposit::getData).collect(Collectors.toList()),
            Integer::new,
            1L << getConstants().getDepositContractTreeDepth().getIntValue());
    state.setEth1Data(new Eth1Data(
        hash_tree_root(deposit_data_list), UInt64.valueOf(deposits.size()), eth1_block_hash));

    // according to the spec deposits are verified before processing
    // but this is redundant for genesis initialisation
    // since passed deposits are created by our own code
    // hence, we are able to avoid verification which saves us some computational resources
    deposits.forEach(deposit -> process_deposit(state, deposit));

    // Process activations
    for (ValidatorIndex index : state.getValidators().size().iterateFromZero()) {
      Gwei balance = state.getBalances().get(index);
      Gwei effective_balance = Gwei.castFrom(
          UInt64s.min(
              balance.minus(balance.modulo(getConstants().getEffectiveBalanceIncrement())),
              getConstants().getMaxEffectiveBalance()
          ));
      state.getValidators().update(index,
          v -> ValidatorRecord.Builder.fromRecord(v)
              .withEffectiveBalance(effective_balance).build());

      ValidatorRecord validator = state.getValidators().get(index);
      if (validator.getEffectiveBalance().equals(getConstants().getMaxEffectiveBalance())) {
        state.getValidators().update(index,
            v -> ValidatorRecord.Builder.fromRecord(v)
                .withActivationEpoch(getConstants().getGenesisEpoch())
                .withActivationEligibilityEpoch(getConstants().getGenesisEpoch()).build());
      }
    }

    return state.createImmutable();
  }

  default Time compute_genesis_time(Time eth1_timestamp) {
    if (!isComputableGenesisTime()) {
      return eth1_timestamp;
    }

    return Time.castFrom(
        eth1_timestamp
            .minus(eth1_timestamp.modulo(getConstants().getSecondsPerDay()))
            .plus(getConstants().getSecondsPerDay().times(2)));
  }

  /*
    def is_valid_genesis_state(state: BeaconState) -> bool:
      if state.genesis_time < MIN_GENESIS_TIME:
          return False
      if len(get_active_validator_indices(state, GENESIS_EPOCH)) < MIN_GENESIS_ACTIVE_VALIDATOR_COUNT:
          return False
      return True
   */
  default boolean is_valid_genesis_state(BeaconState state) {
    if (state.getGenesisTime().less(getConstants().getMinGenesisTime())) {
      return false;
    }
    if (get_active_validator_indices(state, getConstants().getGenesisEpoch()).size()
          < getConstants().getMinGenesisActiveValidatorCount().getIntValue()) {
      return false;
    }
    return true;
  }
}

package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.state.*;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.GWei;
import org.ethereum.beacon.core.types.Shard;
import org.ethereum.beacon.core.types.Slot;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Beacon chain state.
 *
 * @see BeaconBlock
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#beaconstate">BeaconState
 *     in the spec</a>
 */
public interface BeaconState extends Hashable<Hash32> {

  /********* Misc **********/

  /** Slot number that this state was calculated in. */
  Slot getSlot();
  /** Timestamp of the genesis. */
  UInt64 getGenesisTime();
  /** Fork data corresponding to the {@link #getSlot()}. */
  ForkData getForkData();

  /********* Validator registry **********/

  /** Validator registry records. */
  ReadList<ValidatorIndex, ValidatorRecord> getValidatorRegistry();
  /** Validator balances. */
  ReadList<ValidatorIndex, GWei> getValidatorBalances();
  /** Slot number of last validator registry change. */
  Slot getValidatorRegistryLatestChangeSlot();
  /** A nonce for validator registry exits. */
  UInt64 getValidatorRegistryExitCount();
  /** A hash of latest validator registry delta. */
  Hash32 getValidatorRegistryDeltaChainTip();

  /********* Randomness and committees **********/

  /** The most recent randao mixes. */
  ReadList<Integer, Hash32> getLatestRandaoMixes();
  /** The most recent VDF outputs. */
  ReadList<Integer, Hash32> getLatestVdfOutputs();

  Shard getPreviousEpochStartShard();

  Shard getCurrentEpochStartShard();

  Slot getPreviousEpochCalculationSlot();

  Slot getCurrentEpochCalculationSlot();

  Hash32 getPreviousEpochRandaoMix();

  Hash32 getCurrentEpochRandaoMix();

  /** Proof of custody placeholder. */
  ReadList<Integer, CustodyChallenge> getCustodyChallenges();

  /********* Finality **********/

  /** Latest justified slot before {@link #getJustifiedSlot()}. */
  Slot getPreviousJustifiedSlot();
  /** Latest justified slot. */
  Slot getJustifiedSlot();
  /** Bitfield of latest justified slots (epochs). */
  Bitfield getJustificationBitfield();
  /** Latest finalized slot. */
  Slot getFinalizedSlot();

  /********* Recent state **********/

  /** Latest crosslink record for each shard. */
  ReadList<Shard, CrosslinkRecord> getLatestCrosslinks();
  /** Latest block hashes for each shard. */
  ReadList<Integer, Hash32> getLatestBlockRoots();
  /** Indices of validators that has been ejected lately. */
  ReadList<Integer, GWei> getLatestPenalizedExitBalances();
  /** Attestations that has not been processed yet. */
  ReadList<Integer, PendingAttestationRecord> getLatestAttestations();
  /**
   * Latest hashes of {@link #getLatestBlockRoots()} list calculated when
   * its length got exceeded LATEST_BLOCK_ROOTS_LENGTH.
   */
  ReadList<Integer, Hash32> getBatchedBlockRoots();

  /********* PoW receipt root **********/

  /** Latest processed receipt root from PoW deposit contract. */
  Hash32 getLatestDepositRoot();
  /** Receipt roots that voting is still in progress for. */
  ReadList<Integer, DepositRootVote> getDepositRootVotes();

  /**
   * Returns mutable copy of this state.
   * Any changes made to returned copy shouldn't affect this instance
   */
  MutableBeaconState createMutableCopy();

  static BeaconState getEmpty() {
    return createNew(
        UInt64.ZERO,
        UInt64.ZERO,
        ForkData.EMPTY,
        emptyList(),
        emptyList(),
        UInt64.ZERO,
        UInt64.ZERO,
        Hash32.ZERO,
        emptyList(),
        emptyList(),
        UInt64.ZERO,
        UInt64.ZERO,
        UInt64.ZERO,
        UInt64.ZERO,
        Hash32.ZERO,
        Hash32.ZERO,
        emptyList(),
        UInt64.ZERO,
        UInt64.ZERO,
        UInt64.ZERO,
        UInt64.ZERO,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        Hash32.ZERO,
        emptyList());
  }

  static BeaconState createNew(
      UInt64 slot,
      UInt64 genesisTime,
      ForkData forkData,
      List<ValidatorRecord> validatorRegistry,
      List<UInt64> validatorBalances,
      UInt64 validatorRegistryLatestChangeSlot,
      UInt64 validatorRegistryExitCount,
      Hash32 validatorRegistryDeltaChainTip,
      List<Hash32> latestRandaoMixes,
      List<Hash32> latestVdfOutputs,
      UInt64 previousEpochStartShard,
      UInt64 currentEpochStartShard,
      UInt64 previousEpochCalculationSlot,
      UInt64 currentEpochCalculationSlot,
      Hash32 previousEpochRandaoMix,
      Hash32 currentEpochRandaoMix,
      List<CustodyChallenge> custodyChallenges,
      UInt64 previousJustifiedSlot,
      UInt64 justifiedSlot,
      UInt64 justificationBitfield,
      UInt64 finalizedSlot,
      List<CrosslinkRecord> latestCrosslinks,
      List<Hash32> latestBlockRoots,
      List<UInt64> latestPenalizedExitBalances,
      List<PendingAttestationRecord> latestAttestations,
      List<Hash32> batchedBlockRoots,
      Hash32 latestDepositRoot,
      List<DepositRootVote> depositRootVotes) {

    return MutableBeaconState.createNew()
        .withSlot(slot)
        .withGenesisTime(genesisTime)
        .withForkData(forkData)
        .withValidatorRegistry(validatorRegistry)
        .withValidatorBalances(validatorBalances)
        .withValidatorRegistryLatestChangeSlot(validatorRegistryLatestChangeSlot)
        .withValidatorRegistryExitCount(validatorRegistryExitCount)
        .withValidatorRegistryDeltaChainTip(validatorRegistryDeltaChainTip)
        .withLatestRandaoMixes(latestRandaoMixes)
        .withLatestVdfOutputs(latestVdfOutputs)
        .withPreviousEpochStartShard(previousEpochStartShard)
        .withCurrentEpochStartShard(currentEpochStartShard)
        .withPreviousEpochCalculationSlot(previousEpochCalculationSlot)
        .withCurrentEpochCalculationSlot(currentEpochCalculationSlot)
        .withPreviousEpochRandaoMix(previousEpochRandaoMix)
        .withCurrentEpochRandaoMix(currentEpochRandaoMix)
        .withCustodyChallenges(custodyChallenges)
        .withPreviousJustifiedSlot(previousJustifiedSlot)
        .withJustifiedSlot(justifiedSlot)
        .withJustificationBitfield(justificationBitfield)
        .withFinalizedSlot(finalizedSlot)
        .withLatestCrosslinks(latestCrosslinks)
        .withLatestBlockRoots(latestBlockRoots)
        .withLatestPenalizedExitBalances(latestPenalizedExitBalances)
        .withLatestAttestations(latestAttestations)
        .withBatchedBlockRoots(batchedBlockRoots)
        .withLatestDepositRoot(latestDepositRoot)
        .withDepositRootVotes(depositRootVotes)
        .validate();
  }
}

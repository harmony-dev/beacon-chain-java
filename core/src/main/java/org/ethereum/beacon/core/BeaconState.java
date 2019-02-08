package org.ethereum.beacon.core;

import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Beacon chain state.
 *
 * @see BeaconBlock
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#beaconstate">BeaconState
 *     in the spec</a>
 */
public interface BeaconState extends Hashable<Hash32> {

  static BeaconState getEmpty() {
    return new BeaconStateImpl();
  }

  /** ******* Misc ********* */

  /** Slot number that this state was calculated in. */
  SlotNumber getSlot();

  /** ******* Validator registry ********* */

  /** Timestamp of the genesis. */
  Time getGenesisTime();

  /** Fork data corresponding to the {@link #getSlot()}. */
  ForkData getForkData();

  /** Validator registry records. */
  ReadList<ValidatorIndex, ValidatorRecord> getValidatorRegistry();

  /** Validator balances. */
  ReadList<ValidatorIndex, Gwei> getValidatorBalances();

  /** Slot number of last validator registry change. */
  EpochNumber getValidatorRegistryUpdateEpoch();

  /** ******* Randomness and committees ********* */

  /** The most recent randao mixes. */
  ReadList<EpochNumber, Hash32> getLatestRandaoMixes();

  ShardNumber getPreviousEpochStartShard();

  ShardNumber getCurrentEpochStartShard();

  EpochNumber getPreviousCalculationEpoch();

  EpochNumber getCurrentCalculationEpoch();

  Hash32 getPreviousEpochSeed();

  Hash32 getCurrentEpochSeed();

  /********* Finality **********/

  /** Latest justified epoch before {@link #getJustifiedEpoch()}. */
  EpochNumber getPreviousJustifiedEpoch();

  /** Latest justified epoch. */
  EpochNumber getJustifiedEpoch();

  /** Bitfield of latest justified slots (epochs). */
  Bitfield64 getJustificationBitfield();

  /** Latest finalized slot. */
  EpochNumber getFinalizedEpoch();

  /** ******* Recent state ********* */

  /** Latest crosslink record for each shard. */
  ReadList<ShardNumber, CrosslinkRecord> getLatestCrosslinks();

  /** Latest block hashes for each shard. */
  ReadList<SlotNumber, Hash32> getLatestBlockRoots();

  /** Latest block hashes for each shard. */
  ReadList<EpochNumber, Hash32> getLatestIndexRoots();

  /** Balances penalized at every withdrawal period */
  ReadList<EpochNumber, Gwei> getLatestPenalizedBalances();

  /** Attestations that has not been processed yet. */
  ReadList<Integer, PendingAttestationRecord> getLatestAttestations();

  /**
   * Latest hashes of {@link #getLatestBlockRoots()} list calculated when its length got exceeded
   * LATEST_BLOCK_ROOTS_LENGTH.
   */
  ReadList<Integer, Hash32> getBatchedBlockRoots();

  /** ******* PoW receipt root ********* */

  /** Latest processed eth1 data. */
  Eth1Data getLatestEth1Data();

  /** Eth1 data that voting is still in progress for. */
  ReadList<Integer, Eth1DataVote> getEth1DataVotes();

  /**
   * Returns mutable copy of this state. Any changes made to returned copy shouldn't affect this
   * instance
   */
  MutableBeaconState createMutableCopy();

}

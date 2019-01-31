package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
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
  SlotNumber getValidatorRegistryLatestChangeSlot();

  /** ******* Randomness and committees ********* */

  /** A nonce for validator registry exits. */
  UInt64 getValidatorRegistryExitCount();

  /** A hash of latest validator registry delta. */
  Hash32 getValidatorRegistryDeltaChainTip();

  /** The most recent randao mixes. */
  ReadList<UInt64, Hash32> getLatestRandaoMixes();

  /** The most recent VDF outputs. */
  ReadList<Integer, Hash32> getLatestVdfOutputs();

  ShardNumber getPreviousEpochStartShard();

  ShardNumber getCurrentEpochStartShard();

  SlotNumber getPreviousEpochCalculationSlot();

  SlotNumber getCurrentEpochCalculationSlot();

  Hash32 getPreviousEpochRandaoMix();

  /** ******* Finality ********* */
  Hash32 getCurrentEpochRandaoMix();

  /** Proof of custody placeholder. */
  ReadList<Integer, CustodyChallenge> getCustodyChallenges();

  /********* Finality **********/

  /** Latest justified slot before {@link #getJustifiedSlot()}. */
  SlotNumber getPreviousJustifiedSlot();

  /** Latest justified slot. */
  SlotNumber getJustifiedSlot();

  /** ******* Recent state ********* */

  /** Bitfield of latest justified slots (epochs). */
  Bitfield getJustificationBitfield();

  /** Latest finalized slot. */
  SlotNumber getFinalizedSlot();

  /** Latest crosslink record for each shard. */
  ReadList<ShardNumber, CrosslinkRecord> getLatestCrosslinks();

  /** Latest block hashes for each shard. */
  ReadList<SlotNumber, Hash32> getLatestBlockRoots();

  /** Indices of validators that has been ejected lately. */
  ReadList<EpochNumber, Gwei> getLatestPenalizedExitBalances();

  /** ******* PoW receipt root ********* */

  /** Attestations that has not been processed yet. */
  ReadList<Integer, PendingAttestationRecord> getLatestAttestations();

  /**
   * Latest hashes of {@link #getLatestBlockRoots()} list calculated when its length got exceeded
   * LATEST_BLOCK_ROOTS_LENGTH.
   */
  ReadList<Integer, Hash32> getBatchedBlockRoots();

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

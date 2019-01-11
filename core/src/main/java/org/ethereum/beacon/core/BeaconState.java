package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.state.*;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;

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
  UInt64 getSlot();
  /** Timestamp of the genesis. */
  UInt64 getGenesisTime();
  /** Fork data corresponding to the {@link #getSlot()}. */
  ForkData getForkData();

  /********* Validator registry **********/

  /** Validator registry records. */
  List<ValidatorRecord> getValidatorRegistry();
  /** Validator balances. */
  List<UInt64> getValidatorBalances();
  /** Slot number of last validator registry change. */
  UInt64 getValidatorRegistryLatestChangeSlot();
  /** A nonce for validator registry exits. */
  UInt64 getValidatorRegistryExitCount();
  /** A hash of latest validator registry delta. */
  Hash32 getValidatorRegistryDeltaChainTip();

  /********* Randomness and committees **********/

  /** The most recent randao mixes. */
  List<Hash32> getLatestRandaoMixes();
  /** The most recent VDF outputs. */
  List<Hash32> getLatestVdfOutputs();
  /** Which committee assigned to which shard on which slot. */
  List<List<ShardCommittee>> getShardCommitteesAtSlots();
  /** Proof of custody placeholder. */
  List<CustodyChallenge> getCustodyChallenges();

  /********* Finality **********/

  /** Latest justified slot before {@link #getJustifiedSlot()}. */
  UInt64 getPreviousJustifiedSlot();
  /** Latest justified slot. */
  UInt64 getJustifiedSlot();
  /** Bitfield of latest justified slots (epochs). */
  UInt64 getJustificationBitfield();
  /** Latest finalized slot. */
  UInt64 getFinalizedSlot();

  /********* Recent state **********/

  /** Latest crosslink record for each shard. */
  List<CrosslinkRecord> getLatestCrosslinks();
  /** Latest block hashes for each shard. */
  List<Hash32> getLatestBlockRoots();
  /** Indices of validators that has been ejected lately. */
  List<UInt64> getLatestPenalizedExitBalances();
  /** Attestations that has not been processed yet. */
  List<PendingAttestationRecord> getLatestAttestations();
  /**
   * Latest hashes of {@link #getLatestBlockRoots()} list calculated when
   * its length got exceeded LATEST_BLOCK_ROOTS_LENGTH.
   */
  List<Hash32> getBatchedBlockRoots();

  /********* PoW receipt root **********/

  /** Latest processed receipt root from PoW deposit contract. */
  Hash32 getLatestDepositRoot();
  /** Receipt roots that voting is still in progress for. */
  List<DepositRootVote> getDepositRootVotes();
}

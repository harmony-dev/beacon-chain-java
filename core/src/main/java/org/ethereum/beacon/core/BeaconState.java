package org.ethereum.beacon.core;

import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.BeaconStateImpl;
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
public interface BeaconState {

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

  ShardNumber getPreviousShufflingStartShard();

  ShardNumber getCurrentShufflingStartShard();

  EpochNumber getPreviousShufflingEpoch();

  EpochNumber getCurrentShufflingEpoch();

  Hash32 getPreviousShufflingSeed();

  Hash32 getCurrentShufflingSeed();

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
  ReadList<ShardNumber, Crosslink> getLatestCrosslinks();

  /** Latest block hashes for each shard. */
  ReadList<SlotNumber, Hash32> getLatestBlockRoots();

  /** Latest block hashes for each shard. */
  ReadList<EpochNumber, Hash32> getLatestActiveIndexRoots();

  /** Balances slashed at every withdrawal period */
  ReadList<EpochNumber, Gwei> getLatestSlashedBalances();

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

  /** The most recent Eth1 deposit index */
  UInt64 getDepositIndex();

  /**
   * Returns mutable copy of this state. Any changes made to returned copy shouldn't affect this
   * instance
   */
  MutableBeaconState createMutableCopy();

  default boolean equalsHelper(BeaconState other) {
    return getSlot().equals(other.getSlot())
        && getGenesisTime().equals(other.getGenesisTime())
        && getForkData().equals(other.getForkData())
        && getValidatorRegistry().equals(other.getValidatorRegistry())
        && getValidatorBalances().equals(other.getValidatorBalances())
        && getValidatorRegistryUpdateEpoch().equals(other.getValidatorRegistryUpdateEpoch())
        && getLatestRandaoMixes().equals(other.getLatestRandaoMixes())
        && getPreviousShufflingStartShard().equals(other.getPreviousShufflingStartShard())
        && getCurrentShufflingStartShard().equals(other.getCurrentShufflingStartShard())
        && getPreviousShufflingEpoch().equals(other.getPreviousShufflingEpoch())
        && getCurrentShufflingEpoch().equals(other.getCurrentShufflingEpoch())
        && getPreviousShufflingSeed().equals(other.getPreviousShufflingSeed())
        && getCurrentShufflingSeed().equals(other.getCurrentShufflingSeed())
        && getPreviousJustifiedEpoch().equals(other.getPreviousJustifiedEpoch())
        && getJustifiedEpoch().equals(other.getJustifiedEpoch())
        && getJustificationBitfield().equals(other.getJustificationBitfield())
        && getFinalizedEpoch().equals(other.getFinalizedEpoch())
        && getLatestCrosslinks().equals(other.getLatestCrosslinks())
        && getLatestBlockRoots().equals(other.getLatestBlockRoots())
        && getLatestActiveIndexRoots().equals(other.getLatestActiveIndexRoots())
        && getLatestSlashedBalances().equals(other.getLatestSlashedBalances())
        && getLatestAttestations().equals(other.getLatestAttestations())
        && getBatchedBlockRoots().equals(other.getBatchedBlockRoots())
        && getLatestEth1Data().equals(other.getLatestEth1Data())
        && getEth1DataVotes().equals(other.getEth1DataVotes())
        && getDepositIndex().equals(other.getDepositIndex());
  }

  default String toStringShort(@Nullable SpecConstants spec) {
    String ret = "BeaconState["
        + "@ " + getSlot().toString(spec, getGenesisTime())
        + ", " + getForkData().toString(spec)
        + ", validators: " + getValidatorRegistry().size()
        + " updated at epoch " + getValidatorRegistryUpdateEpoch().toString(spec)
        + ", just/final epoch: " + getJustifiedEpoch().toString(spec) + "/" + getFinalizedEpoch().toString(spec);
    if (spec != null) {
      ret += ", latestBlocks=[...";
      for (SlotNumber slot : getSlot().minus(3).iterateTo(getSlot())) {
        Hash32 blockRoot = getLatestBlockRoots().get(slot.modulo(spec.getLatestBlockRootsLength()));
        ret += ", " + blockRoot.toStringShort();
      }
      ret += "]";

      ret += ", attest:["
          + getLatestAttestations().stream().map(ar -> ar.toStringShort(spec)).collect(Collectors.joining(", "))
          + "]";
    }
    ret += "]";

    return ret;
  }
}

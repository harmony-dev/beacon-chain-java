package org.ethereum.beacon.core;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
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
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.0/specs/core/0_beacon-chain.md#beacon-state">BeaconState
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
  Fork getFork();

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

  ReadList<Integer, PendingAttestation> getPreviousEpochAttestations();

  ReadList<Integer, PendingAttestation> getCurrentEpochAttestations();

  /** Latest justified epoch before {@link #getCurrentJustifiedEpoch()}. */
  EpochNumber getPreviousJustifiedEpoch();

  /** Latest justified epoch. */
  EpochNumber getCurrentJustifiedEpoch();

  Hash32 getPreviousJustifiedRoot();

  Hash32 getCurrentJustifiedRoot();

  /** Bitfield of latest justified slots (epochs). */
  Bitfield64 getJustificationBitfield();

  /** Latest finalized slot. */
  EpochNumber getFinalizedEpoch();

  Hash32 getFinalizedRoot();

  /** ******* Recent state ********* */

  /** Latest crosslink record for each shard. */
  ReadList<ShardNumber, Crosslink> getPreviousCrosslinks();

  ReadList<ShardNumber, Crosslink> getCurrentCrosslinks();

  ReadList<SlotNumber, Hash32> getLatestBlockRoots();

  ReadList<SlotNumber, Hash32> getLatestStateRoots();

  ReadList<EpochNumber, Hash32> getLatestActiveIndexRoots();

  /** Balances slashed at every withdrawal period */
  ReadList<EpochNumber, Gwei> getLatestSlashedBalances();

  BeaconBlockHeader getLatestBlockHeader();

  ReadList<Integer, Hash32> getHistoricalRoots();

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

  default String toStringShort(@Nullable SpecConstants spec) {
    String ret = "BeaconState["
        + "@ " + getSlot().toString(spec, getGenesisTime())
        + ", " + getFork().toString(spec)
        + ", validators: " + getValidatorRegistry().size()
        + " updated at epoch " + getValidatorRegistryUpdateEpoch().toString(spec)
        + ", just/final epoch: " + getCurrentJustifiedEpoch().toString(spec) + "/" + getFinalizedEpoch().toString(spec);
    if (spec != null) {
      ret += ", latestBlocks=[...";
      for (SlotNumber slot : getSlot().minus(3).iterateTo(getSlot())) {
        Hash32 blockRoot = getLatestBlockRoots().get(slot.modulo(spec.getSlotsPerHistoricalRoot()));
        ret += ", " + blockRoot.toStringShort();
      }
      ret += "]";

      List<PendingAttestation> attestations = getCurrentEpochAttestations().listCopy();
      attestations.addAll(getPreviousEpochAttestations().listCopy());

      ret += ", attest:["
          + attestations.stream().map(ar -> ar.toStringShort(spec)).collect(Collectors.joining(", "))
          + "]";
    }
    ret += "]";

    return ret;
  }
}

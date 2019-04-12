package org.ethereum.beacon.core;

import java.util.Collections;
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
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.incremental.ObservableComposite;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Beacon chain state.
 *
 * @see BeaconBlock
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.0/specs/core/0_beacon-chain.md#beacon-state">BeaconState
 *     in the spec</a>
 */
public interface BeaconState extends ObservableComposite {

  static BeaconState getEmpty() {
    return getEmpty(new SpecConstants() {});
  }

  static BeaconState getEmpty(SpecConstants specConst) {
    BeaconStateImpl ret = new BeaconStateImpl();
    ret.getLatestRandaoMixes().addAll(
        Collections.nCopies(specConst.getLatestRandaoMixesLength().intValue(), Hash32.ZERO));
    ret.getLatestBlockRoots().addAll(
        Collections.nCopies(specConst.getSlotsPerHistoricalRoot().intValue(), Hash32.ZERO));
    ret.getLatestStateRoots().addAll(
        Collections.nCopies(specConst.getSlotsPerHistoricalRoot().intValue(), Hash32.ZERO));
    ret.getLatestActiveIndexRoots().addAll(
        Collections.nCopies(specConst.getLatestActiveIndexRootsLength().intValue(), Hash32.ZERO));
    ret.getLatestSlashedBalances().addAll(
        Collections.nCopies(specConst.getLatestSlashedExitLength().intValue(), Gwei.ZERO));
    return ret;
  }

  /* ******* Misc ********* */

  /** Slot number that this state was calculated in. */
  @SSZ(order = 0) SlotNumber getSlot();

  /* ******* Validator registry ********* */

  /** Timestamp of the genesis. */
  @SSZ(order = 1) Time getGenesisTime();

  /** Fork data corresponding to the {@link #getSlot()}. */
  @SSZ(order = 2) Fork getFork();

  /** Validator registry records. */
  @SSZ(order = 3) ReadList<ValidatorIndex, ValidatorRecord> getValidatorRegistry();

  /** Validator balances. */
  @SSZ(order = 4) ReadList<ValidatorIndex, Gwei> getValidatorBalances();

  /** Slot number of last validator registry change. */
  @SSZ(order = 5) EpochNumber getValidatorRegistryUpdateEpoch();

  /* ******* Randomness and committees ********* */

  /** The most recent randao mixes. */
  @SSZ(order = 6, vectorSizeVar = "spec.LATEST_RANDAO_MIXES_LENGTH")
  ReadVector<EpochNumber, Hash32> getLatestRandaoMixes();

  @SSZ(order = 7) ShardNumber getPreviousShufflingStartShard();

  @SSZ(order = 8) ShardNumber getCurrentShufflingStartShard();

  @SSZ(order = 9) EpochNumber getPreviousShufflingEpoch();

  @SSZ(order = 10) EpochNumber getCurrentShufflingEpoch();

  @SSZ(order = 11) Hash32 getPreviousShufflingSeed();

  @SSZ(order = 12) Hash32 getCurrentShufflingSeed();

  /********* Finality **********/

  @SSZ(order = 13) ReadList<Integer, PendingAttestation> getPreviousEpochAttestations();

  @SSZ(order = 14) ReadList<Integer, PendingAttestation> getCurrentEpochAttestations();

  /** Latest justified epoch before {@link #getCurrentJustifiedEpoch()}. */
  @SSZ(order = 15) EpochNumber getPreviousJustifiedEpoch();

  /** Latest justified epoch. */
  @SSZ(order = 16) EpochNumber getCurrentJustifiedEpoch();

  @SSZ(order = 17) Hash32 getPreviousJustifiedRoot();

  @SSZ(order = 18) Hash32 getCurrentJustifiedRoot();

  /** Bitfield of latest justified slots (epochs). */
  @SSZ(order = 19) Bitfield64 getJustificationBitfield();

  /** Latest finalized slot. */
  @SSZ(order = 20) EpochNumber getFinalizedEpoch();

  @SSZ(order = 21) Hash32 getFinalizedRoot();

  /* ******* Recent state ********* */

  /** Latest crosslink record for each shard. */
  @SSZ(order = 22) ReadList<ShardNumber, Crosslink> getPreviousCrosslinks();

  @SSZ(order = 23) ReadList<ShardNumber, Crosslink> getCurrentCrosslinks();

  @SSZ(order = 24, vectorSizeVar = "spec.SLOTS_PER_HISTORICAL_ROOT")
  ReadVector<SlotNumber, Hash32> getLatestBlockRoots();

  @SSZ(order = 25, vectorSizeVar = "spec.SLOTS_PER_HISTORICAL_ROOT")
  ReadVector<SlotNumber, Hash32> getLatestStateRoots();

  @SSZ(order = 26, vectorSizeVar = "spec.LATEST_ACTIVE_INDEX_ROOTS_LENGTH")
  ReadVector<EpochNumber, Hash32> getLatestActiveIndexRoots();

  /** Balances slashed at every withdrawal period */
  @SSZ(order = 27, vectorSizeVar = "spec.LATEST_SLASHED_EXIT_LENGTH")
  ReadVector<EpochNumber, Gwei> getLatestSlashedBalances();

  @SSZ(order = 28) BeaconBlockHeader getLatestBlockHeader();

  @SSZ(order = 29) ReadList<Integer, Hash32> getHistoricalRoots();

  /* ******* PoW receipt root ********* */

  /** Latest processed eth1 data. */
  @SSZ(order = 30) Eth1Data getLatestEth1Data();

  /** Eth1 data that voting is still in progress for. */
  @SSZ(order = 31) ReadList<Integer, Eth1DataVote> getEth1DataVotes();

  /** The most recent Eth1 deposit index */
  @SSZ(order = 32) UInt64 getDepositIndex();

  /**
   * Returns mutable copy of this state. Any changes made to returned copy shouldn't affect this
   * instance
   */
  MutableBeaconState createMutableCopy();

  default boolean equalsHelper(BeaconState other) {
    return getSlot().equals(other.getSlot())
        && getGenesisTime().equals(other.getGenesisTime())
        && getFork().equals(other.getFork())
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
        && getPreviousEpochAttestations().equals(other.getPreviousEpochAttestations())
        && getCurrentEpochAttestations().equals(other.getCurrentEpochAttestations())
        && getPreviousJustifiedEpoch().equals(other.getPreviousJustifiedEpoch())
        && getCurrentJustifiedEpoch().equals(other.getCurrentJustifiedEpoch())
        && getPreviousJustifiedRoot().equals(other.getPreviousJustifiedRoot())
        && getCurrentJustifiedRoot().equals(other.getCurrentJustifiedRoot())
        && getJustificationBitfield().equals(other.getJustificationBitfield())
        && getFinalizedEpoch().equals(other.getFinalizedEpoch())
        && getFinalizedRoot().equals(other.getFinalizedRoot())
        && getPreviousCrosslinks().equals(other.getPreviousCrosslinks())
        && getCurrentCrosslinks().equals(other.getCurrentCrosslinks())
        && getLatestBlockRoots().equals(other.getLatestBlockRoots())
        && getLatestStateRoots().equals(other.getLatestStateRoots())
        && getLatestActiveIndexRoots().equals(other.getLatestActiveIndexRoots())
        && getLatestSlashedBalances().equals(other.getLatestSlashedBalances())
        && getLatestBlockHeader().equals(other.getLatestBlockHeader())
        && getHistoricalRoots().equals(other.getHistoricalRoots())
        && getLatestEth1Data().equals(other.getLatestEth1Data())
        && getEth1DataVotes().equals(other.getEth1DataVotes())
        && getDepositIndex().equals(other.getDepositIndex());
  }

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

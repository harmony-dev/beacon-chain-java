package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  /* Misc */

  /** Slot number that this state was calculated in. */
  @SSZ private SlotNumber slot = SlotNumber.ZERO;
  /** Timestamp of the genesis. */
  @SSZ private UInt64 genesisTime = UInt64.ZERO;
  /** Fork data corresponding to the {@link #slot}. */
  @SSZ private ForkData forkData = ForkData.EMPTY;

  /* Validator registry */

  /** Validator registry records. */
  @SSZ private WriteList<ValidatorIndex, ValidatorRecord> validatorRegistry = WriteList.create(ValidatorIndex::of);
  /** Validator balances. */
  @SSZ private WriteList<ValidatorIndex, Gwei>  validatorBalances = WriteList.create(ValidatorIndex::of);
  /** Slot number of last validator registry change. */
  @SSZ private SlotNumber validatorRegistryLatestChangeSlot = SlotNumber.ZERO;
  /** A nonce for validator registry exits. */
  @SSZ private UInt64 validatorRegistryExitCount = UInt64.ZERO;
  /** A hash of latest validator registry delta. */
  @SSZ private Hash32 validatorRegistryDeltaChainTip = Hash32.ZERO;

  /* Randomness and committees */

  /** The most recent randao mixes. */
  @SSZ private WriteList<UInt64, Hash32> latestRandaoMixes = WriteList.create(UInt64::valueOf);
  /** The most recent VDF outputs. */
  @SSZ private WriteList<Integer, Hash32> latestVdfOutputs = WriteList.create(Integer::valueOf);

  @SSZ private ShardNumber previousEpochStartShard = ShardNumber.ZERO;
  @SSZ private ShardNumber currentEpochStartShard = ShardNumber.ZERO;
  @SSZ private SlotNumber previousEpochCalculationSlot = SlotNumber.ZERO;
  @SSZ private SlotNumber currentEpochCalculationSlot = SlotNumber.ZERO;
  @SSZ private Hash32 previousEpochRandaoMix = Hash32.ZERO;
  @SSZ private Hash32 currentEpochRandaoMix = Hash32.ZERO;

  /** Proof of custody placeholder. */
  @SSZ private WriteList<Integer, CustodyChallenge> custodyChallenges = WriteList.create(Integer::valueOf);

  /* Finality */

  /** Latest justified slot before {@link #justifiedSlot}. */
  @SSZ private SlotNumber previousJustifiedSlot = SlotNumber.ZERO;
  /** Latest justified slot. */
  @SSZ private SlotNumber justifiedSlot = SlotNumber.ZERO;
  /** Bitfield of latest justified slots (epochs). */
  @SSZ private Bitfield justificationBitfield = Bitfield.ZERO;
  /** Latest finalized slot. */
  @SSZ private SlotNumber finalizedSlot = SlotNumber.ZERO;

  /* Recent state */

  /** Latest crosslink record for each shard. */
  @SSZ private WriteList<ShardNumber, CrosslinkRecord> latestCrosslinks = WriteList.create(ShardNumber::of);
  /** Latest block hashes for each shard. */
  @SSZ private WriteList<Integer, Hash32> latestBlockRoots = WriteList.create(Integer::valueOf);
  /** Indices of validators that has been ejected lately. */
  @SSZ private WriteList<EpochNumber, Gwei> latestPenalizedExitBalances = WriteList.create(EpochNumber::of);
  /** Attestations that has not been processed yet. */
  @SSZ private WriteList<Integer, PendingAttestationRecord> latestAttestations = WriteList.create(Integer::valueOf);
  /**
   * Latest hashes of {@link #latestBlockRoots} list calculated when its length got exceeded
   * LATEST_BLOCK_ROOTS_LENGTH.
   */
  @SSZ private WriteList<Integer, Hash32> batchedBlockRoots = WriteList.create(Integer::valueOf);

  /* PoW receipt root */

  /** Latest processed eth1 data. */
  @SSZ private Eth1Data latestEth1Data = Eth1Data.EMPTY;
  /** Eth1 data items that voting is still in progress for. */
  @SSZ private WriteList<Integer, Eth1DataVote> eth1DataVotes = WriteList.create(Integer::valueOf);

  public BeaconStateImpl() {}

  private BeaconStateImpl(BeaconState state) {
        slot = state.getSlot();
        genesisTime = state.getGenesisTime();
        forkData = state.getForkData();

        validatorRegistry = state.getValidatorRegistry().createMutableCopy();
        validatorBalances = state.getValidatorBalances().createMutableCopy();
        validatorRegistryLatestChangeSlot = state.getValidatorRegistryLatestChangeSlot();
        validatorRegistryExitCount = state.getValidatorRegistryExitCount();
        validatorRegistryDeltaChainTip = state.getValidatorRegistryDeltaChainTip();

        latestRandaoMixes = state.getLatestRandaoMixes().createMutableCopy();
        latestVdfOutputs = state.getLatestVdfOutputs().createMutableCopy();

        previousEpochStartShard = state.getPreviousEpochStartShard();
        currentEpochStartShard = state.getCurrentEpochStartShard();
        previousEpochCalculationSlot = state.getPreviousEpochCalculationSlot();
        currentEpochCalculationSlot = state.getCurrentEpochCalculationSlot();
        previousEpochRandaoMix = state.getPreviousEpochRandaoMix();
        currentEpochRandaoMix = state.getCurrentEpochRandaoMix();

        custodyChallenges = state.getCustodyChallenges().createMutableCopy();

        previousJustifiedSlot = state.getPreviousJustifiedSlot();
        justifiedSlot = state.getJustifiedSlot();
        justificationBitfield = state.getJustificationBitfield();
        finalizedSlot = state.getFinalizedSlot();

        latestCrosslinks = state.getLatestCrosslinks().createMutableCopy();
        latestBlockRoots = state.getLatestBlockRoots().createMutableCopy();
        latestPenalizedExitBalances = state.getLatestPenalizedExitBalances().createMutableCopy();
        latestAttestations = state.getLatestAttestations().createMutableCopy();
        batchedBlockRoots = state.getBatchedBlockRoots().createMutableCopy();

        latestEth1Data = state.getLatestEth1Data();
        eth1DataVotes = state.getEth1DataVotes().createMutableCopy();
  }

  @Override
  public BeaconState createImmutable() {
    // TODO validation
    return new BeaconStateImpl(this);
  }

  @Override
  public SlotNumber getSlot() {
    return slot;
  }

  @Override
  public UInt64 getGenesisTime() {
    return genesisTime;
  }

  @Override
  public ForkData getForkData() {
    return forkData;
  }

  @Override
  public WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry() {
    return validatorRegistry;
  }

  @Override
  public WriteList<ValidatorIndex, Gwei> getValidatorBalances() {
    return validatorBalances;
  }

  @Override
  public SlotNumber getValidatorRegistryLatestChangeSlot() {
    return validatorRegistryLatestChangeSlot;
  }

  @Override
  public UInt64 getValidatorRegistryExitCount() {
    return validatorRegistryExitCount;
  }

  @Override
  public Hash32 getValidatorRegistryDeltaChainTip() {
    return validatorRegistryDeltaChainTip;
  }

  @Override
  public WriteList<UInt64, Hash32> getLatestRandaoMixes() {
    return latestRandaoMixes;
  }

  @Override
  public WriteList<Integer, Hash32> getLatestVdfOutputs() {
    return latestVdfOutputs;
  }

  @Override
  public ShardNumber getPreviousEpochStartShard() {
    return previousEpochStartShard;
  }

  @Override
  public ShardNumber getCurrentEpochStartShard() {
    return currentEpochStartShard;
  }

  @Override
  public SlotNumber getPreviousEpochCalculationSlot() {
    return previousEpochCalculationSlot;
  }

  @Override
  public SlotNumber getCurrentEpochCalculationSlot() {
    return currentEpochCalculationSlot;
  }

  @Override
  public Hash32 getPreviousEpochRandaoMix() {
    return previousEpochRandaoMix;
  }

  @Override
  public Hash32 getCurrentEpochRandaoMix() {
    return currentEpochRandaoMix;
  }

  @Override
  public WriteList<Integer, CustodyChallenge> getCustodyChallenges() {
    return custodyChallenges;
  }

  @Override
  public SlotNumber getPreviousJustifiedSlot() {
    return previousJustifiedSlot;
  }

  @Override
  public SlotNumber getJustifiedSlot() {
    return justifiedSlot;
  }

  @Override
  public Bitfield getJustificationBitfield() {
    return justificationBitfield;
  }

  @Override
  public SlotNumber getFinalizedSlot() {
    return finalizedSlot;
  }

  @Override
  public WriteList<ShardNumber, CrosslinkRecord> getLatestCrosslinks() {
    return latestCrosslinks;
  }

  @Override
  public WriteList<Integer, Hash32> getLatestBlockRoots() {
    return latestBlockRoots;
  }

  @Override
  public WriteList<EpochNumber, Gwei> getLatestPenalizedExitBalances() {
    return latestPenalizedExitBalances;
  }

  @Override
  public WriteList<Integer, PendingAttestationRecord> getLatestAttestations() {
    return latestAttestations;
  }

  @Override
  public WriteList<Integer, Hash32> getBatchedBlockRoots() {
    return batchedBlockRoots;
  }

  @Override
  public Eth1Data getLatestEth1Data() {
    return latestEth1Data;
  }

  @Override
  public WriteList<Integer, Eth1DataVote> getEth1DataVotes() {
    return eth1DataVotes;
  }

  @Override
  public void setSlot(SlotNumber slot) {
    this.slot = slot;
  }

  @Override
  public void setGenesisTime(UInt64 genesisTime) {
    this.genesisTime = genesisTime;
  }

  @Override
  public void setForkData(ForkData forkData) {
    this.forkData = forkData;
  }

  @Override
  public void setValidatorRegistryLatestChangeSlot(
      SlotNumber validatorRegistryLatestChangeSlot) {
    this.validatorRegistryLatestChangeSlot = validatorRegistryLatestChangeSlot;
  }

  @Override
  public void setValidatorRegistryExitCount(
      UInt64 validatorRegistryExitCount) {
    this.validatorRegistryExitCount = validatorRegistryExitCount;
  }

  @Override
  public void setValidatorRegistryDeltaChainTip(
      Hash32 validatorRegistryDeltaChainTip) {
    this.validatorRegistryDeltaChainTip = validatorRegistryDeltaChainTip;
  }

  @Override
  public void setPreviousEpochStartShard(
      ShardNumber previousEpochStartShard) {
    this.previousEpochStartShard = previousEpochStartShard;
  }

  @Override
  public void setCurrentEpochStartShard(ShardNumber currentEpochStartShard) {
    this.currentEpochStartShard = currentEpochStartShard;
  }

  @Override
  public void setPreviousEpochCalculationSlot(
      SlotNumber previousEpochCalculationSlot) {
    this.previousEpochCalculationSlot = previousEpochCalculationSlot;
  }

  @Override
  public void setCurrentEpochCalculationSlot(
      SlotNumber currentEpochCalculationSlot) {
    this.currentEpochCalculationSlot = currentEpochCalculationSlot;
  }

  @Override
  public void setPreviousEpochRandaoMix(Hash32 previousEpochRandaoMix) {
    this.previousEpochRandaoMix = previousEpochRandaoMix;
  }

  @Override
  public void setCurrentEpochRandaoMix(Hash32 currentEpochRandaoMix) {
    this.currentEpochRandaoMix = currentEpochRandaoMix;
  }

  @Override
  public void setPreviousJustifiedSlot(SlotNumber previousJustifiedSlot) {
    this.previousJustifiedSlot = previousJustifiedSlot;
  }

  @Override
  public void setJustifiedSlot(SlotNumber justifiedSlot) {
    this.justifiedSlot = justifiedSlot;
  }

  @Override
  public void setJustificationBitfield(Bitfield justificationBitfield) {
    this.justificationBitfield = justificationBitfield;
  }

  @Override
  public void setFinalizedSlot(SlotNumber finalizedSlot) {
    this.finalizedSlot = finalizedSlot;
  }

  @Override
  public void setLatestEth1Data(Eth1Data latestEth1Data) {
    this.latestEth1Data = latestEth1Data;
  }

  @Override
  public Hash32 getHash() {
    return Hash32.ZERO;
  }

  @Override
  public MutableBeaconState createMutableCopy() {
    return new BeaconStateImpl(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BeaconStateImpl that = (BeaconStateImpl) o;
    return Objects.equal(slot, that.slot)
        && Objects.equal(genesisTime, that.genesisTime)
        && Objects.equal(forkData, that.forkData)
        && Objects.equal(validatorRegistry, that.validatorRegistry)
        && Objects.equal(validatorBalances, that.validatorBalances)
        && Objects.equal(validatorRegistryLatestChangeSlot, that.validatorRegistryLatestChangeSlot)
        && Objects.equal(validatorRegistryExitCount, that.validatorRegistryExitCount)
        && Objects.equal(validatorRegistryDeltaChainTip, that.validatorRegistryDeltaChainTip)
        && Objects.equal(latestRandaoMixes, that.latestRandaoMixes)
        && Objects.equal(latestVdfOutputs, that.latestVdfOutputs)
        && Objects.equal(previousEpochStartShard, that.previousEpochStartShard)
        && Objects.equal(currentEpochStartShard, that.currentEpochStartShard)
        && Objects.equal(previousEpochCalculationSlot, that.previousEpochCalculationSlot)
        && Objects.equal(currentEpochCalculationSlot, that.currentEpochCalculationSlot)
        && Objects.equal(previousEpochRandaoMix, that.previousEpochRandaoMix)
        && Objects.equal(currentEpochRandaoMix, that.currentEpochRandaoMix)
        && Objects.equal(custodyChallenges, that.custodyChallenges)
        && Objects.equal(previousJustifiedSlot, that.previousJustifiedSlot)
        && Objects.equal(justifiedSlot, that.justifiedSlot)
        && Objects.equal(justificationBitfield, that.justificationBitfield)
        && Objects.equal(finalizedSlot, that.finalizedSlot)
        && Objects.equal(latestCrosslinks, that.latestCrosslinks)
        && Objects.equal(latestBlockRoots, that.latestBlockRoots)
        && Objects.equal(latestPenalizedExitBalances, that.latestPenalizedExitBalances)
        && Objects.equal(latestAttestations, that.latestAttestations)
        && Objects.equal(batchedBlockRoots, that.batchedBlockRoots)
        && Objects.equal(latestEth1Data, that.latestEth1Data)
        && Objects.equal(eth1DataVotes, that.eth1DataVotes);
  }
}

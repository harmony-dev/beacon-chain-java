package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.ArrayList;
import java.util.List;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  /* Misc */

  /** Slot number that this state was calculated in. */
  @SSZ private SlotNumber slot = SlotNumber.ZERO;
  /** Timestamp of the genesis. */
  @SSZ private Time genesisTime = Time.ZERO;
  /** Fork data corresponding to the {@link #slot}. */
  @SSZ private ForkData forkData = ForkData.EMPTY;

  /* Validator registry */

  /** Validator registry records. */
  @SSZ private List<ValidatorRecord> validatorRegistryList = new ArrayList<>();
  /** Validator balances. */
  @SSZ private List<Gwei> validatorBalancesList = new ArrayList<>();
  /** Slot number of last validator registry change. */
  @SSZ private SlotNumber validatorRegistryLatestChangeSlot = SlotNumber.ZERO;
  /** A nonce for validator registry exits. */
  @SSZ private UInt64 validatorRegistryExitCount = UInt64.ZERO;
  /** A hash of latest validator registry delta. */
  @SSZ private Hash32 validatorRegistryDeltaChainTip = Hash32.ZERO;

  /* Randomness and committees */

  /** The most recent randao mixes. */
  @SSZ private List<Hash32> latestRandaoMixesList = new ArrayList<>();
  /** The most recent VDF outputs. */
  @SSZ private List<Hash32> latestVdfOutputsList = new ArrayList<>();

  @SSZ private ShardNumber previousEpochStartShard = ShardNumber.ZERO;
  @SSZ private ShardNumber currentEpochStartShard = ShardNumber.ZERO;
  @SSZ private SlotNumber previousEpochCalculationSlot = SlotNumber.ZERO;
  @SSZ private SlotNumber currentEpochCalculationSlot = SlotNumber.ZERO;
  @SSZ private Hash32 previousEpochRandaoMix = Hash32.ZERO;
  @SSZ private Hash32 currentEpochRandaoMix = Hash32.ZERO;

  /** Proof of custody placeholder. */
  @SSZ private List<CustodyChallenge> custodyChallengesList = new ArrayList<>();

  /* Finality */

  /** Latest justified slot before {@link #justifiedSlot}. */
  @SSZ private SlotNumber previousJustifiedSlot = SlotNumber.ZERO;
  /** Latest justified slot. */
  @SSZ private SlotNumber justifiedSlot = SlotNumber.ZERO;
  /** Bitfield of latest justified slots (epochs). */
  @SSZ private Bitfield64 justificationBitfield = Bitfield64.ZERO;
  /** Latest finalized slot. */
  @SSZ private SlotNumber finalizedSlot = SlotNumber.ZERO;

  /* Recent state */

  /** Latest crosslink record for each shard. */
  @SSZ private List<CrosslinkRecord> latestCrosslinksList = new ArrayList<>();
  /** Latest block hashes for each shard. */
  @SSZ private List<Hash32> latestBlockRootsList = new ArrayList<>();
  /** Indices of validators that has been ejected lately. */
  @SSZ private List<Gwei> latestPenalizedExitBalancesList = new ArrayList<>();
  /** Attestations that has not been processed yet. */
  @SSZ private List<PendingAttestationRecord> latestAttestationsList = new ArrayList<>();
  /**
   * Latest hashes of {@link #latestBlockRootsList} list calculated when its length got exceeded
   * LATEST_BLOCK_ROOTS_LENGTH.
   */
  @SSZ private List<Hash32> batchedBlockRootsList = new ArrayList<>();

  /* PoW receipt root */

  /** Latest processed eth1 data. */
  @SSZ private Eth1Data latestEth1Data = Eth1Data.EMPTY;
  /** Eth1 data items that voting is still in progress for. */
  @SSZ private List<Eth1DataVote> eth1DataVotesList = new ArrayList<>();

  public BeaconStateImpl() {}

  private BeaconStateImpl(BeaconState state) {
        slot = state.getSlot();
        genesisTime = state.getGenesisTime();
        forkData = state.getForkData();

        validatorRegistryList = state.getValidatorRegistry().listCopy();
        validatorBalancesList = state.getValidatorBalances().listCopy();
        validatorRegistryLatestChangeSlot = state.getValidatorRegistryLatestChangeSlot();
        validatorRegistryExitCount = state.getValidatorRegistryExitCount();
        validatorRegistryDeltaChainTip = state.getValidatorRegistryDeltaChainTip();

        latestRandaoMixesList = state.getLatestRandaoMixes().listCopy();
        latestVdfOutputsList = state.getLatestVdfOutputs().listCopy();

        previousEpochStartShard = state.getPreviousEpochStartShard();
        currentEpochStartShard = state.getCurrentEpochStartShard();
        previousEpochCalculationSlot = state.getPreviousEpochCalculationSlot();
        currentEpochCalculationSlot = state.getCurrentEpochCalculationSlot();
        previousEpochRandaoMix = state.getPreviousEpochRandaoMix();
        currentEpochRandaoMix = state.getCurrentEpochRandaoMix();

        custodyChallengesList = state.getCustodyChallenges().listCopy();

        previousJustifiedSlot = state.getPreviousJustifiedSlot();
        justifiedSlot = state.getJustifiedSlot();
        justificationBitfield = state.getJustificationBitfield();
        finalizedSlot = state.getFinalizedSlot();

        latestCrosslinksList = state.getLatestCrosslinks().listCopy();
        latestBlockRootsList = state.getLatestBlockRoots().listCopy();
        latestPenalizedExitBalancesList = state.getLatestPenalizedExitBalances().listCopy();
        latestAttestationsList = state.getLatestAttestations().listCopy();
        batchedBlockRootsList = state.getBatchedBlockRoots().listCopy();

        latestEth1Data = state.getLatestEth1Data();
        eth1DataVotesList = state.getEth1DataVotes().listCopy();
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
  public Time getGenesisTime() {
    return genesisTime;
  }

  @Override
  public ForkData getForkData() {
    return forkData;
  }

  @Override
  public WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry() {
    return WriteList.wrap(validatorRegistryList, ValidatorIndex::of);
  }

  @Override
  public WriteList<ValidatorIndex, Gwei> getValidatorBalances() {
    return WriteList.wrap(validatorBalancesList, ValidatorIndex::of);
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
    return WriteList.wrap(latestRandaoMixesList, UInt64::valueOf);
  }

  @Override
  public WriteList<Integer, Hash32> getLatestVdfOutputs() {
    return WriteList.wrap(latestVdfOutputsList, Integer::valueOf);
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
    return WriteList.wrap(custodyChallengesList, Integer::valueOf);
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
  public Bitfield64 getJustificationBitfield() {
    return justificationBitfield;
  }

  @Override
  public SlotNumber getFinalizedSlot() {
    return finalizedSlot;
  }

  @Override
  public WriteList<ShardNumber, CrosslinkRecord> getLatestCrosslinks() {
    return WriteList.wrap(latestCrosslinksList, ShardNumber::of);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestBlockRoots() {
    return WriteList.wrap(latestBlockRootsList, SlotNumber::of);
  }

  @Override
  public WriteList<EpochNumber, Gwei> getLatestPenalizedExitBalances() {
    return WriteList.wrap(latestPenalizedExitBalancesList, EpochNumber::of);
  }

  @Override
  public WriteList<Integer, PendingAttestationRecord> getLatestAttestations() {
    return WriteList.wrap(latestAttestationsList, Integer::valueOf);
  }

  @Override
  public WriteList<Integer, Hash32> getBatchedBlockRoots() {
    return WriteList.wrap(batchedBlockRootsList, Integer::valueOf);
  }

  @Override
  public Eth1Data getLatestEth1Data() {
    return latestEth1Data;
  }

  @Override
  public WriteList<Integer, Eth1DataVote> getEth1DataVotes() {
    return WriteList.wrap(eth1DataVotesList, Integer::valueOf);
  }

  @Override
  public void setSlot(SlotNumber slot) {
    this.slot = slot;
  }

  @Override
  public void setGenesisTime(Time genesisTime) {
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
  public void setJustificationBitfield(Bitfield64 justificationBitfield) {
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

  public List<ValidatorRecord> getValidatorRegistryList() {
    return validatorRegistryList;
  }

  public void setValidatorRegistryList(
      List<ValidatorRecord> validatorRegistryList) {
    this.validatorRegistryList = validatorRegistryList;
  }

  public List<Gwei> getValidatorBalancesList() {
    return validatorBalancesList;
  }

  public void setValidatorBalancesList(
      List<Gwei> validatorBalancesList) {
    this.validatorBalancesList = validatorBalancesList;
  }

  public List<Hash32> getLatestRandaoMixesList() {
    return latestRandaoMixesList;
  }

  public void setLatestRandaoMixesList(
      List<Hash32> latestRandaoMixesList) {
    this.latestRandaoMixesList = latestRandaoMixesList;
  }

  public List<Hash32> getLatestVdfOutputsList() {
    return latestVdfOutputsList;
  }

  public void setLatestVdfOutputsList(
      List<Hash32> latestVdfOutputsList) {
    this.latestVdfOutputsList = latestVdfOutputsList;
  }

  public List<CustodyChallenge> getCustodyChallengesList() {
    return custodyChallengesList;
  }

  public void setCustodyChallengesList(
      List<CustodyChallenge> custodyChallengesList) {
    this.custodyChallengesList = custodyChallengesList;
  }

  public List<CrosslinkRecord> getLatestCrosslinksList() {
    return latestCrosslinksList;
  }

  public void setLatestCrosslinksList(
      List<CrosslinkRecord> latestCrosslinksList) {
    this.latestCrosslinksList = latestCrosslinksList;
  }

  public List<Hash32> getLatestBlockRootsList() {
    return latestBlockRootsList;
  }

  public void setLatestBlockRootsList(
      List<Hash32> latestBlockRootsList) {
    this.latestBlockRootsList = latestBlockRootsList;
  }

  public List<Gwei> getLatestPenalizedExitBalancesList() {
    return latestPenalizedExitBalancesList;
  }

  public void setLatestPenalizedExitBalancesList(
      List<Gwei> latestPenalizedExitBalancesList) {
    this.latestPenalizedExitBalancesList = latestPenalizedExitBalancesList;
  }

  public List<PendingAttestationRecord> getLatestAttestationsList() {
    return latestAttestationsList;
  }

  public void setLatestAttestationsList(
      List<PendingAttestationRecord> latestAttestationsList) {
    this.latestAttestationsList = latestAttestationsList;
  }

  public List<Hash32> getBatchedBlockRootsList() {
    return batchedBlockRootsList;
  }

  public void setBatchedBlockRootsList(
      List<Hash32> batchedBlockRootsList) {
    this.batchedBlockRootsList = batchedBlockRootsList;
  }

  public List<Eth1DataVote> getEth1DataVotesList() {
    return eth1DataVotesList;
  }

  public void setEth1DataVotesList(
      List<Eth1DataVote> eth1DataVotesList) {
    this.eth1DataVotesList = eth1DataVotesList;
  }

  /*********  List Getters/Setter for serialization  **********/



  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BeaconStateImpl that = (BeaconStateImpl) o;
    return Objects.equal(slot, that.slot)
        && Objects.equal(genesisTime, that.genesisTime)
        && Objects.equal(forkData, that.forkData)
        && Objects.equal(validatorRegistryList, that.validatorRegistryList)
        && Objects.equal(validatorBalancesList, that.validatorBalancesList)
        && Objects.equal(validatorRegistryLatestChangeSlot, that.validatorRegistryLatestChangeSlot)
        && Objects.equal(validatorRegistryExitCount, that.validatorRegistryExitCount)
        && Objects.equal(validatorRegistryDeltaChainTip, that.validatorRegistryDeltaChainTip)
        && Objects.equal(latestRandaoMixesList, that.latestRandaoMixesList)
        && Objects.equal(latestVdfOutputsList, that.latestVdfOutputsList)
        && Objects.equal(previousEpochStartShard, that.previousEpochStartShard)
        && Objects.equal(currentEpochStartShard, that.currentEpochStartShard)
        && Objects.equal(previousEpochCalculationSlot, that.previousEpochCalculationSlot)
        && Objects.equal(currentEpochCalculationSlot, that.currentEpochCalculationSlot)
        && Objects.equal(previousEpochRandaoMix, that.previousEpochRandaoMix)
        && Objects.equal(currentEpochRandaoMix, that.currentEpochRandaoMix)
        && Objects.equal(custodyChallengesList, that.custodyChallengesList)
        && Objects.equal(previousJustifiedSlot, that.previousJustifiedSlot)
        && Objects.equal(justifiedSlot, that.justifiedSlot)
        && Objects.equal(justificationBitfield, that.justificationBitfield)
        && Objects.equal(finalizedSlot, that.finalizedSlot)
        && Objects.equal(latestCrosslinksList, that.latestCrosslinksList)
        && Objects.equal(latestBlockRootsList, that.latestBlockRootsList)
        && Objects.equal(latestPenalizedExitBalancesList, that.latestPenalizedExitBalancesList)
        && Objects.equal(latestAttestationsList, that.latestAttestationsList)
        && Objects.equal(batchedBlockRootsList, that.batchedBlockRootsList)
        && Objects.equal(latestEth1Data, that.latestEth1Data)
        && Objects.equal(eth1DataVotesList, that.eth1DataVotesList);
  }
}

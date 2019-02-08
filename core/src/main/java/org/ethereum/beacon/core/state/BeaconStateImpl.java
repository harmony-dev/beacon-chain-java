package org.ethereum.beacon.core.state;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.Serializer;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.WriteList;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  /* Misc */

  @SSZ private SlotNumber slot = SlotNumber.ZERO;
  @SSZ private Time genesisTime = Time.ZERO;
  @SSZ private ForkData forkData = ForkData.EMPTY;

  /* Validator registry */

  @SSZ private List<ValidatorRecord> validatorRegistryList = new ArrayList<>();
  @SSZ private List<Gwei> validatorBalancesList = new ArrayList<>();
  @SSZ private EpochNumber validatorRegistryUpdateEpoch = EpochNumber.ZERO;

  /* Randomness and committees */

  @SSZ private List<Hash32> latestRandaoMixesList = new ArrayList<>();
  @SSZ private ShardNumber previousEpochStartShard = ShardNumber.ZERO;
  @SSZ private ShardNumber currentEpochStartShard = ShardNumber.ZERO;
  @SSZ private EpochNumber previousCalculationEpoch = EpochNumber.ZERO;
  @SSZ private EpochNumber currentCalculationEpoch = EpochNumber.ZERO;
  @SSZ private Hash32 previousEpochSeed = Hash32.ZERO;
  @SSZ private Hash32 currentEpochSeed = Hash32.ZERO;

  /* Finality */

  @SSZ private EpochNumber previousJustifiedEpoch = EpochNumber.ZERO;
  @SSZ private EpochNumber justifiedEpoch = EpochNumber.ZERO;
  @SSZ private Bitfield64 justificationBitfield = Bitfield64.ZERO;
  @SSZ private EpochNumber finalizedEpoch = EpochNumber.ZERO;

  /* Recent state */

  @SSZ private List<CrosslinkRecord> latestCrosslinksList = new ArrayList<>();
  @SSZ private List<Hash32> latestBlockRootsList = new ArrayList<>();
  @SSZ private List<Hash32> latestIndexRootsList = new ArrayList<>();
  @SSZ private List<Gwei> latestPenalizedBalancesList = new ArrayList<>();
  @SSZ private List<PendingAttestationRecord> latestAttestationsList = new ArrayList<>();
  @SSZ private List<Hash32> batchedBlockRootsList = new ArrayList<>();

  /* PoW receipt root */

  @SSZ private Eth1Data latestEth1Data = Eth1Data.EMPTY;
  @SSZ private List<Eth1DataVote> eth1DataVotesList = new ArrayList<>();

  public BeaconStateImpl() {}

  private BeaconStateImpl(BeaconState state) {
        slot = state.getSlot();
        genesisTime = state.getGenesisTime();
        forkData = state.getForkData();

        validatorRegistryList = state.getValidatorRegistry().listCopy();
        validatorBalancesList = state.getValidatorBalances().listCopy();
        validatorRegistryUpdateEpoch = state.getValidatorRegistryUpdateEpoch();

        latestRandaoMixesList = state.getLatestRandaoMixes().listCopy();
        previousEpochStartShard = state.getPreviousEpochStartShard();
        currentEpochStartShard = state.getCurrentEpochStartShard();
        previousCalculationEpoch = state.getPreviousCalculationEpoch();
        currentCalculationEpoch = state.getCurrentCalculationEpoch();
        previousEpochSeed = state.getPreviousEpochSeed();
        currentEpochSeed = state.getCurrentEpochSeed();

        previousJustifiedEpoch = state.getPreviousJustifiedEpoch();
        justifiedEpoch = state.getJustifiedEpoch();
        justificationBitfield = state.getJustificationBitfield();
        finalizedEpoch = state.getFinalizedEpoch();

        latestCrosslinksList = state.getLatestCrosslinks().listCopy();
        latestBlockRootsList = state.getLatestBlockRoots().listCopy();
        latestIndexRootsList = state.getLatestIndexRoots().listCopy();
        latestPenalizedBalancesList = state.getLatestPenalizedBalances().listCopy();
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
  public void setSlot(SlotNumber slot) {
    this.slot = slot;
  }

  @Override
  public Time getGenesisTime() {
    return genesisTime;
  }

  @Override
  public void setGenesisTime(Time genesisTime) {
    this.genesisTime = genesisTime;
  }

  @Override
  public ForkData getForkData() {
    return forkData;
  }

  @Override
  public void setForkData(ForkData forkData) {
    this.forkData = forkData;
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

  @Override
  public EpochNumber getValidatorRegistryUpdateEpoch() {
    return validatorRegistryUpdateEpoch;
  }

  @Override
  public void setValidatorRegistryUpdateEpoch(
      EpochNumber validatorRegistryUpdateEpoch) {
    this.validatorRegistryUpdateEpoch = validatorRegistryUpdateEpoch;
  }

  public List<Hash32> getLatestRandaoMixesList() {
    return latestRandaoMixesList;
  }

  public void setLatestRandaoMixesList(
      List<Hash32> latestRandaoMixesList) {
    this.latestRandaoMixesList = latestRandaoMixesList;
  }

  @Override
  public ShardNumber getPreviousEpochStartShard() {
    return previousEpochStartShard;
  }

  @Override
  public void setPreviousEpochStartShard(
      ShardNumber previousEpochStartShard) {
    this.previousEpochStartShard = previousEpochStartShard;
  }

  @Override
  public ShardNumber getCurrentEpochStartShard() {
    return currentEpochStartShard;
  }

  @Override
  public void setCurrentEpochStartShard(ShardNumber currentEpochStartShard) {
    this.currentEpochStartShard = currentEpochStartShard;
  }

  @Override
  public EpochNumber getPreviousCalculationEpoch() {
    return previousCalculationEpoch;
  }

  @Override
  public void setPreviousCalculationEpoch(
      EpochNumber previousCalculationEpoch) {
    this.previousCalculationEpoch = previousCalculationEpoch;
  }

  @Override
  public EpochNumber getCurrentCalculationEpoch() {
    return currentCalculationEpoch;
  }

  @Override
  public void setCurrentCalculationEpoch(
      EpochNumber currentCalculationEpoch) {
    this.currentCalculationEpoch = currentCalculationEpoch;
  }

  @Override
  public Hash32 getPreviousEpochSeed() {
    return previousEpochSeed;
  }

  @Override
  public void setPreviousEpochSeed(Hash32 previousEpochSeed) {
    this.previousEpochSeed = previousEpochSeed;
  }

  @Override
  public Hash32 getCurrentEpochSeed() {
    return currentEpochSeed;
  }

  @Override
  public void setCurrentEpochSeed(Hash32 currentEpochSeed) {
    this.currentEpochSeed = currentEpochSeed;
  }

  @Override
  public EpochNumber getPreviousJustifiedEpoch() {
    return previousJustifiedEpoch;
  }

  @Override
  public void setPreviousJustifiedEpoch(EpochNumber previousJustifiedEpoch) {
    this.previousJustifiedEpoch = previousJustifiedEpoch;
  }

  @Override
  public EpochNumber getJustifiedEpoch() {
    return justifiedEpoch;
  }

  public void setJustifiedEpoch(EpochNumber justifiedEpoch) {
    this.justifiedEpoch = justifiedEpoch;
  }

  @Override
  public Bitfield64 getJustificationBitfield() {
    return justificationBitfield;
  }

  @Override
  public void setJustificationBitfield(Bitfield64 justificationBitfield) {
    this.justificationBitfield = justificationBitfield;
  }

  @Override
  public EpochNumber getFinalizedEpoch() {
    return finalizedEpoch;
  }

  @Override
  public void setFinalizedEpoch(EpochNumber finalizedEpoch) {
    this.finalizedEpoch = finalizedEpoch;
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

  public List<Hash32> getLatestIndexRootsList() {
    return latestIndexRootsList;
  }

  public void setLatestIndexRootsList(
      List<Hash32> latestIndexRootsList) {
    this.latestIndexRootsList = latestIndexRootsList;
  }

  public List<Gwei> getLatestPenalizedBalancesList() {
    return latestPenalizedBalancesList;
  }

  public void setLatestPenalizedBalancesList(
      List<Gwei> latestPenalizedBalancesList) {
    this.latestPenalizedBalancesList = latestPenalizedBalancesList;
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

  @Override
  public Eth1Data getLatestEth1Data() {
    return latestEth1Data;
  }

  @Override
  public void setLatestEth1Data(Eth1Data latestEth1Data) {
    this.latestEth1Data = latestEth1Data;
  }

  public List<Eth1DataVote> getEth1DataVotesList() {
    return eth1DataVotesList;
  }

  public void setEth1DataVotesList(
      List<Eth1DataVote> eth1DataVotesList) {
    this.eth1DataVotesList = eth1DataVotesList;
  }

  @Override
  public WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry() {
    return WriteList.wrap(getValidatorRegistryList(), ValidatorIndex::of);
  }

  @Override
  public WriteList<ValidatorIndex, Gwei> getValidatorBalances() {
    return WriteList.wrap(getValidatorBalancesList(), ValidatorIndex::of);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestRandaoMixes() {
    return WriteList.wrap(getLatestRandaoMixesList(), EpochNumber::of);
  }

  @Override
  public WriteList<ShardNumber, CrosslinkRecord> getLatestCrosslinks() {
    return WriteList.wrap(getLatestCrosslinksList(), ShardNumber::of);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestBlockRoots() {
    return WriteList.wrap(getLatestBlockRootsList(), SlotNumber::of);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestIndexRoots() {
    return WriteList.wrap(getLatestIndexRootsList(), EpochNumber::of);
  }

  @Override
  public WriteList<EpochNumber, Gwei> getLatestPenalizedBalances() {
    return WriteList.wrap(getLatestPenalizedBalancesList(), EpochNumber::of);
  }

  @Override
  public WriteList<Integer, PendingAttestationRecord> getLatestAttestations() {
    return WriteList.wrap(getLatestAttestationsList(), Integer::valueOf);
  }

  @Override
  public WriteList<Integer, Hash32> getBatchedBlockRoots() {
    return WriteList.wrap(getBatchedBlockRootsList(), Integer::valueOf);
  }

  @Override
  public WriteList<Integer, Eth1DataVote> getEth1DataVotes() {
    return WriteList.wrap(getEth1DataVotesList(), Integer::valueOf);
  }

  /*********  List Getters/Setter for serialization  **********/

  @Override
  public MutableBeaconState createMutableCopy() {
    return new BeaconStateImpl(this);
  }


  @Override
  public boolean equals(Object o) {
    Serializer serializer = Serializer.annotationSerializer();
    return serializer.encode2(this).equals(serializer.encode2(o));
  }
}

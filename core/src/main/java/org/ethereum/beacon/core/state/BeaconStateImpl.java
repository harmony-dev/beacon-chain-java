package org.ethereum.beacon.core.state;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
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
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  /* Misc */

  @SSZ private SlotNumber slot = SlotNumber.ZERO;
  @SSZ private Time genesisTime = Time.ZERO;
  @SSZ private Fork fork = Fork.EMPTY;

  /* Validator registry */

  @SSZ private List<ValidatorRecord> validatorRegistryList = new ArrayList<>();
  @SSZ private List<Gwei> validatorBalancesList = new ArrayList<>();
  @SSZ private EpochNumber validatorRegistryUpdateEpoch = EpochNumber.ZERO;

  /* Randomness and committees */

  @SSZ private List<Hash32> latestRandaoMixesList = new ArrayList<>();
  @SSZ private ShardNumber previousShufflingStartShard = ShardNumber.ZERO;
  @SSZ private ShardNumber currentShufflingStartShard = ShardNumber.ZERO;
  @SSZ private EpochNumber previousShufflingEpoch = EpochNumber.ZERO;
  @SSZ private EpochNumber currentShufflingEpoch = EpochNumber.ZERO;
  @SSZ private Hash32 previousShufflingSeed = Hash32.ZERO;
  @SSZ private Hash32 currentShufflingSeed = Hash32.ZERO;

  /* Finality */

  @SSZ private List<PendingAttestation> previousEpochAttestationList = new ArrayList<>();
  @SSZ private List<PendingAttestation> currentEpochAttestationList = new ArrayList<>();
  @SSZ private EpochNumber previousJustifiedEpoch = EpochNumber.ZERO;
  @SSZ private EpochNumber currentJustifiedEpoch = EpochNumber.ZERO;
  @SSZ private Hash32 previousJustifiedRoot = Hash32.ZERO;
  @SSZ private Hash32 currentJustifiedRoot = Hash32.ZERO;
  @SSZ private Bitfield64 justificationBitfield = Bitfield64.ZERO;
  @SSZ private EpochNumber finalizedEpoch = EpochNumber.ZERO;
  @SSZ private Hash32 finalizedRoot = Hash32.ZERO;

  /* Recent state */

  @SSZ private List<Crosslink> latestCrosslinksList = new ArrayList<>();
  @SSZ private List<Hash32> latestBlockRootsList = new ArrayList<>();
  @SSZ private List<Hash32> latestStateRootsList = new ArrayList<>();
  @SSZ private List<Hash32> latestActiveIndexRootsList = new ArrayList<>();
  @SSZ private List<Gwei> latestSlashedBalancesList = new ArrayList<>();
  @SSZ private BeaconBlockHeader latestBlockHeader = BeaconBlockHeader.EMPTY;
  @SSZ private List<Hash32> historicalRootList = new ArrayList<>();

  /* PoW receipt root */

  @SSZ private Eth1Data latestEth1Data = Eth1Data.EMPTY;
  @SSZ private List<Eth1DataVote> eth1DataVotesList = new ArrayList<>();
  @SSZ private UInt64 depositIndex = UInt64.ZERO;

  public BeaconStateImpl() {}

  BeaconStateImpl(BeaconState state) {
    slot = state.getSlot();
    genesisTime = state.getGenesisTime();
    fork = state.getFork();

    validatorRegistryList = state.getValidatorRegistry().listCopy();
    validatorBalancesList = state.getValidatorBalances().listCopy();
    validatorRegistryUpdateEpoch = state.getValidatorRegistryUpdateEpoch();

    latestRandaoMixesList = state.getLatestRandaoMixes().listCopy();
    previousShufflingStartShard = state.getPreviousShufflingStartShard();
    currentShufflingStartShard = state.getCurrentShufflingStartShard();
    previousShufflingEpoch = state.getPreviousShufflingEpoch();
    currentShufflingEpoch = state.getCurrentShufflingEpoch();
    previousShufflingSeed = state.getPreviousShufflingSeed();
    currentShufflingSeed = state.getCurrentShufflingSeed();

    previousEpochAttestationList = state.getPreviousEpochAttestations().listCopy();
    currentEpochAttestationList = state.getCurrentEpochAttestations().listCopy();
    previousJustifiedEpoch = state.getPreviousJustifiedEpoch();
    currentJustifiedEpoch = state.getCurrentJustifiedEpoch();
    previousJustifiedRoot = state.getPreviousJustifiedRoot();
    currentJustifiedRoot = state.getCurrentJustifiedRoot();
    justificationBitfield = state.getJustificationBitfield();
    finalizedEpoch = state.getFinalizedEpoch();
    finalizedRoot = state.getFinalizedRoot();

    latestCrosslinksList = state.getLatestCrosslinks().listCopy();
    latestBlockRootsList = state.getLatestBlockRoots().listCopy();
    latestStateRootsList = state.getLatestStateRoots().listCopy();
    latestActiveIndexRootsList = state.getLatestActiveIndexRoots().listCopy();
    latestSlashedBalancesList = state.getLatestSlashedBalances().listCopy();
    latestBlockHeader = state.getLatestBlockHeader();
    historicalRootList = state.getHistoricalRoots().listCopy();

    latestEth1Data = state.getLatestEth1Data();
    eth1DataVotesList = state.getEth1DataVotes().listCopy();
    depositIndex = state.getDepositIndex();
  }

  @Override
  public BeaconState createImmutable() {
    return new ImmutableBeaconStateImpl(this);
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
  public Fork getFork() {
    return fork;
  }

  @Override
  public void setFork(Fork fork) {
    this.fork = fork;
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
  public ShardNumber getPreviousShufflingStartShard() {
    return previousShufflingStartShard;
  }

  @Override
  public void setPreviousShufflingStartShard(
      ShardNumber previousShufflingStartShard) {
    this.previousShufflingStartShard = previousShufflingStartShard;
  }

  @Override
  public ShardNumber getCurrentShufflingStartShard() {
    return currentShufflingStartShard;
  }

  @Override
  public void setCurrentShufflingStartShard(ShardNumber currentShufflingStartShard) {
    this.currentShufflingStartShard = currentShufflingStartShard;
  }

  @Override
  public EpochNumber getPreviousShufflingEpoch() {
    return previousShufflingEpoch;
  }

  @Override
  public void setPreviousShufflingEpoch(
      EpochNumber previousShufflingEpoch) {
    this.previousShufflingEpoch = previousShufflingEpoch;
  }

  @Override
  public EpochNumber getCurrentShufflingEpoch() {
    return currentShufflingEpoch;
  }

  @Override
  public void setCurrentShufflingEpoch(
      EpochNumber currentShufflingEpoch) {
    this.currentShufflingEpoch = currentShufflingEpoch;
  }

  @Override
  public Hash32 getPreviousShufflingSeed() {
    return previousShufflingSeed;
  }

  @Override
  public void setPreviousShufflingSeed(Hash32 previousShufflingSeed) {
    this.previousShufflingSeed = previousShufflingSeed;
  }

  @Override
  public Hash32 getCurrentShufflingSeed() {
    return currentShufflingSeed;
  }

  @Override
  public void setCurrentShufflingSeed(Hash32 currentShufflingSeed) {
    this.currentShufflingSeed = currentShufflingSeed;
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
  public EpochNumber getCurrentJustifiedEpoch() {
    return currentJustifiedEpoch;
  }

  @Override
  public Hash32 getPreviousJustifiedRoot() {
    return previousJustifiedRoot;
  }

  @Override
  public Hash32 getCurrentJustifiedRoot() {
    return currentJustifiedRoot;
  }

  public void setCurrentJustifiedEpoch(EpochNumber justifiedEpoch) {
    this.currentJustifiedEpoch = justifiedEpoch;
  }

  @Override
  public void setPreviousJustifiedRoot(Hash32 previousJustifiedRoot) {
    this.previousJustifiedRoot = previousJustifiedRoot;
  }

  @Override
  public void setCurrentJustifiedRoot(Hash32 currentJustifiedRoot) {
    this.currentJustifiedRoot = currentJustifiedRoot;
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
  public Hash32 getFinalizedRoot() {
    return finalizedRoot;
  }

  @Override
  public void setFinalizedEpoch(EpochNumber finalizedEpoch) {
    this.finalizedEpoch = finalizedEpoch;
  }

  @Override
  public void setFinalizedRoot(Hash32 finalizedRoot) {
    this.finalizedRoot = finalizedRoot;
  }

  @Override
  public void setLatestBlockHeader(BeaconBlockHeader latestBlockHeader) {
    this.latestBlockHeader = latestBlockHeader;
  }

  public List<Crosslink> getLatestCrosslinksList() {
    return latestCrosslinksList;
  }

  public void setLatestCrosslinksList(
      List<Crosslink> latestCrosslinksList) {
    this.latestCrosslinksList = latestCrosslinksList;
  }

  public List<Hash32> getLatestBlockRootsList() {
    return latestBlockRootsList;
  }

  public List<PendingAttestation> getPreviousEpochAttestationList() {
    return previousEpochAttestationList;
  }

  public List<PendingAttestation> getCurrentEpochAttestationList() {
    return currentEpochAttestationList;
  }

  public List<Hash32> getLatestStateRootsList() {
    return latestStateRootsList;
  }

  public List<Hash32> getHistoricalRootList() {
    return historicalRootList;
  }

  public void setLatestBlockRootsList(
      List<Hash32> latestBlockRootsList) {
    this.latestBlockRootsList = latestBlockRootsList;
  }

  public List<Hash32> getLatestActiveIndexRootsList() {
    return latestActiveIndexRootsList;
  }

  public void setLatestActiveIndexRootsList(
      List<Hash32> latestActiveIndexRootsList) {
    this.latestActiveIndexRootsList = latestActiveIndexRootsList;
  }

  public List<Gwei> getLatestSlashedBalancesList() {
    return latestSlashedBalancesList;
  }

  public void setLatestSlashedBalancesList(
      List<Gwei> latestSlashedBalancesList) {
    this.latestSlashedBalancesList = latestSlashedBalancesList;
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

  public void setPreviousEpochAttestationList(
      List<PendingAttestation> previousEpochAttestationList) {
    this.previousEpochAttestationList = previousEpochAttestationList;
  }

  public void setCurrentEpochAttestationList(
      List<PendingAttestation> currentEpochAttestationList) {
    this.currentEpochAttestationList = currentEpochAttestationList;
  }

  public void setLatestStateRootsList(
      List<Hash32> latestStateRootsList) {
    this.latestStateRootsList = latestStateRootsList;
  }

  public void setHistoricalRootList(
      List<Hash32> historicalRootList) {
    this.historicalRootList = historicalRootList;
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
  public WriteList<ShardNumber, Crosslink> getLatestCrosslinks() {
    return WriteList.wrap(getLatestCrosslinksList(), ShardNumber::of);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestBlockRoots() {
    return WriteList.wrap(getLatestBlockRootsList(), SlotNumber::of);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestStateRoots() {
    return WriteList.wrap(getLatestStateRootsList(), SlotNumber::of);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestActiveIndexRoots() {
    return WriteList.wrap(getLatestActiveIndexRootsList(), EpochNumber::of);
  }

  @Override
  public WriteList<EpochNumber, Gwei> getLatestSlashedBalances() {
    return WriteList.wrap(getLatestSlashedBalancesList(), EpochNumber::of);
  }

  @Override
  public BeaconBlockHeader getLatestBlockHeader() {
    return latestBlockHeader;
  }

  @Override
  public WriteList<Integer, PendingAttestation> getPreviousEpochAttestations() {
    return WriteList.wrap(getPreviousEpochAttestationList(), Function.identity());
  }

  @Override
  public WriteList<Integer, PendingAttestation> getCurrentEpochAttestations() {
    return WriteList.wrap(getCurrentEpochAttestationList(), Function.identity());
  }

  @Override
  public WriteList<Integer, Hash32> getHistoricalRoots() {
    return WriteList.wrap(getHistoricalRootList(), Function.identity());
  }

  @Override
  public WriteList<Integer, Eth1DataVote> getEth1DataVotes() {
    return WriteList.wrap(getEth1DataVotesList(), Integer::valueOf);
  }

  @Override
  public UInt64 getDepositIndex() {
    return depositIndex;
  }

  @Override
  public void setDepositIndex(UInt64 depositIndex) {
    this.depositIndex = depositIndex;
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

  @Override
  public String toString() {
    return toStringShort(null);
  }
}

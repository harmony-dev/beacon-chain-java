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
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  /* Misc */

  private SlotNumber slot = SlotNumber.ZERO;
  private Time genesisTime = Time.ZERO;
  private Fork fork = Fork.EMPTY;

  /* Validator registry */

  private WriteList<ValidatorIndex, ValidatorRecord> validatorRegistry =
      WriteList.create(ValidatorIndex::of);
  private WriteList<ValidatorIndex, Gwei> validatorBalances =
      WriteList.create(ValidatorIndex::of);
  private EpochNumber validatorRegistryUpdateEpoch = EpochNumber.ZERO;

  /* Randomness and committees */

  private WriteList<EpochNumber, Hash32> latestRandaoMixes =
      WriteList.create(EpochNumber::of);
  private ShardNumber previousShufflingStartShard = ShardNumber.ZERO;
  private ShardNumber currentShufflingStartShard = ShardNumber.ZERO;
  private EpochNumber previousShufflingEpoch = EpochNumber.ZERO;
  private EpochNumber currentShufflingEpoch = EpochNumber.ZERO;
  private Hash32 previousShufflingSeed = Hash32.ZERO;
  private Hash32 currentShufflingSeed = Hash32.ZERO;

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

  @SSZ private List<Crosslink> previousCrosslinksList = new ArrayList<>();
  @SSZ private List<Crosslink> currentCrosslinksList = new ArrayList<>();
  @SSZ private List<Hash32> latestBlockRootsList = new ArrayList<>();
  @SSZ private List<Hash32> latestStateRootsList = new ArrayList<>();
  @SSZ private List<Hash32> latestActiveIndexRootsList = new ArrayList<>();
  @SSZ private List<Gwei> latestSlashedBalancesList = new ArrayList<>();
  @SSZ private BeaconBlockHeader latestBlockHeader = BeaconBlockHeader.EMPTY;
  @SSZ private List<Hash32> historicalRootList = new ArrayList<>();

  /* PoW receipt root */

  private Eth1Data latestEth1Data = Eth1Data.EMPTY;
  private WriteList<Integer, Eth1DataVote> eth1DataVotes =
      WriteList.create(Integer::valueOf);
  private UInt64 depositIndex = UInt64.ZERO;

  public BeaconStateImpl() {}

  BeaconStateImpl(BeaconState state) {
    slot = state.getSlot();
    genesisTime = state.getGenesisTime();
    fork = state.getFork();

        validatorRegistry = state.getValidatorRegistry().createMutableCopy();
        validatorBalances = state.getValidatorBalances().createMutableCopy();
        validatorRegistryUpdateEpoch = state.getValidatorRegistryUpdateEpoch();

        latestRandaoMixes = state.getLatestRandaoMixes().createMutableCopy();
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

    previousCrosslinksList = state.getPreviousCrosslinks().listCopy();
    currentCrosslinksList = state.getCurrentCrosslinks().listCopy();
    latestBlockRootsList = state.getLatestBlockRoots().listCopy();
    latestStateRootsList = state.getLatestStateRoots().listCopy();
    latestActiveIndexRootsList = state.getLatestActiveIndexRoots().listCopy();
    latestSlashedBalancesList = state.getLatestSlashedBalances().listCopy();
    latestBlockHeader = state.getLatestBlockHeader();
    historicalRootList = state.getHistoricalRoots().listCopy();

        latestEth1Data = state.getLatestEth1Data();
        eth1DataVotes = state.getEth1DataVotes().createMutableCopy();depositIndex = state.getDepositIndex();
  }

  @Override
  public BeaconState createImmutable() {
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
  public Fork getFork() {
    return fork;
  }

  @Override
  public void setFork(Fork fork) {
    this.fork = fork;
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
  public Eth1Data getLatestEth1Data() {
    return latestEth1Data;
  }

  @Override
  public void setLatestEth1Data(Eth1Data latestEth1Data) {
    this.latestEth1Data = latestEth1Data;
  }

  @Override
  public UInt64 getDepositIndex() {
    return depositIndex;
  }

  @Override
  public void setDepositIndex(UInt64 depositIndex) {
    this.depositIndex = depositIndex;
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
    return validatorRegistry;
  }

  public void setValidatorRegistry(
      WriteList<ValidatorIndex, ValidatorRecord> validatorRegistry) {
    this.validatorRegistry = validatorRegistry;
  }

  @Override
  public WriteList<ValidatorIndex, Gwei> getValidatorBalances() {
    return validatorBalances;
  }

  public void setValidatorBalances(
      WriteList<ValidatorIndex, Gwei> validatorBalances) {
    this.validatorBalances = validatorBalances;
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestRandaoMixes() {
    return latestRandaoMixes;
  }

  public void setLatestRandaoMixes(
      WriteList<EpochNumber, Hash32> latestRandaoMixes) {
    this.latestRandaoMixes = latestRandaoMixes;
  }

  @Override
  public WriteList<ShardNumber, Crosslink> getPreviousCrosslinks() {
    return WriteList.wrap(getPreviousCrosslinksList(), ShardNumber::of);
  }

  @Override
  public WriteList<ShardNumber, Crosslink> getCurrentCrosslinks() {
    return WriteList.wrap(getCurrentCrosslinksList(), ShardNumber::of);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestBlockRoots() {
    return latestBlockRoots;
  }

  public void setLatestBlockRoots(
      WriteList<SlotNumber, Hash32> latestBlockRoots) {
    this.latestBlockRoots = latestBlockRoots;
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestStateRoots() {
    return WriteList.wrap(getLatestStateRootsList(), SlotNumber::of);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestActiveIndexRoots() {
    return latestActiveIndexRoots;
  }

  public void setLatestActiveIndexRoots(
      WriteList<EpochNumber, Hash32> latestActiveIndexRoots) {
    this.latestActiveIndexRoots = latestActiveIndexRoots;
  }

  @Override
  public WriteList<EpochNumber, Gwei> getLatestSlashedBalances() {
    return latestSlashedBalances;
  }

  public void setLatestSlashedBalances(
      WriteList<EpochNumber, Gwei> latestSlashedBalances) {
    this.latestSlashedBalances = latestSlashedBalances;
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
    return eth1DataVotes;
  }

  public void setEth1DataVotes(
      WriteList<Integer, Eth1DataVote> eth1DataVotes) {
    this.eth1DataVotes = eth1DataVotes;
  }

  /*********  List Getters/Setter for serialization  **********/



  @Override
  public MutableBeaconState createMutableCopy() {
    return new BeaconStateImpl(this);
  }

  @Override
  public boolean equals(Object obj) {
    return equalsHelper((BeaconState) obj);
  }

  @Override
  public String toString() {
    return toStringShort(null);
  }
}

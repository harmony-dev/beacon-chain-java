package org.ethereum.beacon.core.state;

import java.util.Map;
import java.util.function.Supplier;
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
import org.ethereum.beacon.ssz.incremental.ObservableCompositeHelper;
import org.ethereum.beacon.ssz.incremental.ObservableCompositeHelper.ObsValue;
import org.ethereum.beacon.ssz.incremental.ObservableListImpl;
import org.ethereum.beacon.ssz.incremental.UpdateListener;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.collections.WriteVector;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  private ObservableCompositeHelper obsHelper = new ObservableCompositeHelper();

  /* Versioning */
  private ObsValue<Time> genesisTime = obsHelper.newValue(Time.ZERO);
  private ObsValue<SlotNumber> slot = obsHelper.newValue(SlotNumber.ZERO);
  private ObsValue<Fork> fork = obsHelper.newValue(Fork.EMPTY);

  /* History */
  private ObsValue<BeaconBlockHeader> latestBlockHeader =
      obsHelper.newValue(BeaconBlockHeader.EMPTY);
  private ObsValue<WriteList<SlotNumber, Hash32>> blockRoots =
      obsHelper.newValue(ObservableListImpl.create(SlotNumber::of, true));
  private ObsValue<WriteList<SlotNumber, Hash32>> stateRoots =
      obsHelper.newValue(ObservableListImpl.create(SlotNumber::of, true));
  private ObsValue<WriteList<Integer, Hash32>> historicalRoots =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));

  /* Eth1 */
  private ObsValue<Eth1Data> eth1Data = obsHelper.newValue(Eth1Data.EMPTY);
  private ObsValue<WriteList<Integer, Eth1Data>> eth1DataVotes =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));
  private ObsValue<UInt64> eth1DepositIndex = obsHelper.newValue(UInt64.ZERO);

  /* Registry */
  private ObsValue<WriteList<ValidatorIndex, ValidatorRecord>> validators =
      obsHelper.newValue(ObservableListImpl.create(ValidatorIndex::of));
  private ObsValue<WriteList<ValidatorIndex, Gwei>> balances =
      obsHelper.newValue(ObservableListImpl.create(ValidatorIndex::of));

  /* Shuffling */
  private ObsValue<ShardNumber> startShard = obsHelper.newValue(ShardNumber.ZERO);
  private ObsValue<WriteList<EpochNumber, Hash32>> randaoMixes =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of, true));
  private ObsValue<WriteList<EpochNumber, Hash32>> activeIndexRoots =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of, true));
  private ObsValue<WriteList<EpochNumber, Hash32>> compactCommitteesRoots =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of, true));

  /* Slashings */
  private ObsValue<WriteList<EpochNumber, Gwei>> slashings =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of, true));

  /* Attestations */
  private ObsValue<WriteList<Integer, PendingAttestation>> previousEpochAttestations =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));
  private ObsValue<WriteList<Integer, PendingAttestation>> currentEpochAttestations =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));

  /* Crosslinks */
  private ObsValue<WriteList<ShardNumber, Crosslink>> previousCrosslinks =
      obsHelper.newValue(ObservableListImpl.create(ShardNumber::of, true));
  private ObsValue<WriteList<ShardNumber, Crosslink>> currentCrosslinks =
      obsHelper.newValue(ObservableListImpl.create(ShardNumber::of, true));


  /* Finality */
  private ObsValue<Bitfield64> justificationBits = obsHelper.newValue(Bitfield64.ZERO);
  private ObsValue<Checkpoint> previousJustifiedCheckpoint = obsHelper.newValue(Checkpoint.EMPTY);
  private ObsValue<Checkpoint> currentJustifiedCheckpoint = obsHelper.newValue(Checkpoint.EMPTY);
  private ObsValue<Checkpoint> finalizedCheckpoint = obsHelper.newValue(Checkpoint.EMPTY);

  public BeaconStateImpl() {}

  BeaconStateImpl(BeaconState state) {
    genesisTime.set(state.getGenesisTime());
    slot.set(state.getSlot());
    fork.set(state.getFork());

    latestBlockHeader.set(state.getLatestBlockHeader());
    blockRoots.set(state.getBlockRoots().createMutableCopy());
    stateRoots.set(state.getStateRoots().createMutableCopy());
    historicalRoots.set(state.getHistoricalRoots().createMutableCopy());

    eth1Data.set(state.getEth1Data());
    eth1DataVotes.set(state.getEth1DataVotes().createMutableCopy());
    eth1DepositIndex.set(state.getEth1DepositIndex());

    validators.set(state.getValidators().createMutableCopy());
    balances.set(state.getBalances().createMutableCopy());

    startShard.set(state.getStartShard());
    randaoMixes.set(state.getRandaoMixes().createMutableCopy());
    activeIndexRoots.set(state.getActiveIndexRoots().createMutableCopy());
    compactCommitteesRoots.set(state.getCompactCommitteesRoots().createMutableCopy());

    slashings.set(state.getSlashings().createMutableCopy());

    previousEpochAttestations.set(state.getPreviousEpochAttestations().createMutableCopy());
    currentEpochAttestations.set(state.getCurrentEpochAttestations().createMutableCopy());

    previousCrosslinks.set(state.getPreviousCrosslinks().createMutableCopy());
    currentCrosslinks.set(state.getCurrentCrosslinks().createMutableCopy());

    justificationBits.set(state.getJustificationBits());
    previousJustifiedCheckpoint.set(state.getPreviousJustifiedCheckpoint());
    currentJustifiedCheckpoint.set(state.getCurrentJustifiedCheckpoint());
    finalizedCheckpoint.set(state.getFinalizedCheckpoint());

    obsHelper.addAllListeners(state.getAllUpdateListeners());
  }

  @Override
  public Map<String, UpdateListener> getAllUpdateListeners() {
    return obsHelper.getAllUpdateListeners();
  }

  @Override
  public UpdateListener getUpdateListener(String observerId, Supplier<UpdateListener> listenerFactory) {
    return obsHelper.getUpdateListener(observerId, listenerFactory);
  }

  @Override
  public BeaconState createImmutable() {
    return new BeaconStateImpl(this);
  }

  @Override
  public SlotNumber getSlot() {
    return slot.get();
  }

  @Override
  public void setSlot(SlotNumber slot) {
    this.slot.set(slot);
  }

  @Override
  public Time getGenesisTime() {
    return genesisTime.get();
  }

  @Override
  public void setGenesisTime(Time genesisTime) {
    this.genesisTime.set(genesisTime);
  }

  @Override
  public Fork getFork() {
    return fork.get();
  }

  @Override
  public void setFork(Fork fork) {
    this.fork.set(fork);
  }

  @Override
  public WriteList<ValidatorIndex, ValidatorRecord> getValidators() {
    return validators.get();
  }

  public void setValidators(
      WriteList<ValidatorIndex, ValidatorRecord> validators) {
    this.validators.get().replaceAll(validators);
  }

  @Override
  public WriteList<ValidatorIndex, Gwei> getBalances() {
    return balances.get();
  }

  public void setBalances(
      WriteList<ValidatorIndex, Gwei> balances) {
    this.balances.get().replaceAll(balances);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getRandaoMixes() {
    return randaoMixes.get();
  }

  public void setRandaoMixes(
      WriteList<EpochNumber, Hash32> randaoMixes) {
    this.randaoMixes.get().replaceAll(randaoMixes);
  }

  @Override
  public ShardNumber getStartShard() {
    return startShard.get();
  }

  @Override
  public void setStartShard(ShardNumber startShard) {
    this.startShard.set(startShard);
  }

  public WriteList<Integer, PendingAttestation> getPreviousEpochAttestations() {
    return previousEpochAttestations.get();
  }

  public void setPreviousEpochAttestations(
      WriteList<Integer, PendingAttestation> previousEpochAttestations) {
    this.previousEpochAttestations.get().replaceAll(previousEpochAttestations);
  }

  public WriteList<Integer, PendingAttestation> getCurrentEpochAttestations() {
    return currentEpochAttestations.get();
  }

  public void setCurrentEpochAttestations(
      WriteList<Integer, PendingAttestation> currentEpochAttestations) {
    this.currentEpochAttestations.get().replaceAll(currentEpochAttestations);
  }

  @Override
  public Bitfield64 getJustificationBits() {
    return justificationBits.get();
  }

  @Override
  public void setJustificationBits(Bitfield64 justificationBits) {
    this.justificationBits.set(justificationBits);
  }

  @Override
  public WriteList<ShardNumber, Crosslink> getPreviousCrosslinks() {
    return previousCrosslinks.get();
  }

  public void setPreviousCrosslinks(
      WriteList<ShardNumber, Crosslink> previousCrosslinks) {
    this.previousCrosslinks.get().replaceAll(previousCrosslinks);
  }

  @Override
  public WriteList<ShardNumber, Crosslink> getCurrentCrosslinks() {
    return currentCrosslinks.get();
  }

  public void setCurrentCrosslinks(
      WriteList<ShardNumber, Crosslink> currentCrosslinks) {
    this.currentCrosslinks.get().replaceAll(currentCrosslinks);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getBlockRoots() {
    return blockRoots.get();
  }

  public void setBlockRoots(
      WriteList<SlotNumber, Hash32> blockRoots) {
    this.blockRoots.get().replaceAll(blockRoots);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getStateRoots() {
    return stateRoots.get();
  }

  public void setStateRoots(
      WriteList<SlotNumber, Hash32> stateRoots) {
    this.stateRoots.get().replaceAll(stateRoots);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getActiveIndexRoots() {
    return activeIndexRoots.get();
  }

  public void setActiveIndexRoots(
      WriteList<EpochNumber, Hash32> activeIndexRoots) {
    this.activeIndexRoots.get().replaceAll(activeIndexRoots);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getCompactCommitteesRoots() {
    return compactCommitteesRoots.get();
  }

  public void setCompactCommitteesRoots(WriteList<EpochNumber, Hash32> compactCommitteesRoots) {
    this.compactCommitteesRoots.get().replaceAll(compactCommitteesRoots);
  }

  @Override
  public WriteList<EpochNumber, Gwei> getSlashings() {
    return slashings.get();
  }

  public void setSlashings(
      WriteList<EpochNumber, Gwei> slashings) {
    this.slashings.get().replaceAll(slashings);
  }

  @Override
  public BeaconBlockHeader getLatestBlockHeader() {
    return latestBlockHeader.get();
  }

  @Override
  public void setLatestBlockHeader(BeaconBlockHeader latestBlockHeader) {
    this.latestBlockHeader.set(latestBlockHeader);
  }

  public WriteList<Integer, Hash32> getHistoricalRoots() {
    return historicalRoots.get();
  }

  public void setHistoricalRoots(
      WriteList<Integer, Hash32> historicalRoots) {
    this.historicalRoots.get().replaceAll(historicalRoots);
  }

  @Override
  public Eth1Data getEth1Data() {
    return eth1Data.get();
  }

  @Override
  public void setEth1Data(Eth1Data latestEth1Data) {
    this.eth1Data.set(latestEth1Data);
  }

  @Override
  public WriteList<Integer, Eth1Data> getEth1DataVotes() {
    return eth1DataVotes.get();
  }

  @Override
  public void setEth1DataVotes(
      WriteList<Integer, Eth1Data> eth1DataVotes) {
    this.eth1DataVotes.get().replaceAll(eth1DataVotes);
  }

  @Override
  public UInt64 getEth1DepositIndex() {
    return eth1DepositIndex.get();
  }

  @Override
  public void setEth1DepositIndex(UInt64 depositIndex) {
    this.eth1DepositIndex.set(depositIndex);
  }

  @Override
  public Checkpoint getPreviousJustifiedCheckpoint() {
    return previousJustifiedCheckpoint.get();
  }

  @Override
  public Checkpoint getCurrentJustifiedCheckpoint() {
    return currentJustifiedCheckpoint.get();
  }

  @Override
  public Checkpoint getFinalizedCheckpoint() {
    return finalizedCheckpoint.get();
  }

  public void setPreviousJustifiedCheckpoint(Checkpoint previousJustifiedCheckpoint) {
    this.previousJustifiedCheckpoint.set(previousJustifiedCheckpoint);
  }

  public void setCurrentJustifiedCheckpoint(Checkpoint currentJustifiedCheckpoint) {
    this.currentJustifiedCheckpoint.set(currentJustifiedCheckpoint);
  }

  public void setFinalizedCheckpoint(Checkpoint finalizedCheckpoint) {
    this.finalizedCheckpoint.set(finalizedCheckpoint);
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

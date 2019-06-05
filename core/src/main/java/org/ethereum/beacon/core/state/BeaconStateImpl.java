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
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  private ObservableCompositeHelper obsHelper = new ObservableCompositeHelper();

  /* Misc */

  private ObsValue<SlotNumber> slot = obsHelper.newValue(SlotNumber.ZERO);
  private ObsValue<Time> genesisTime = obsHelper.newValue(Time.ZERO);
  private ObsValue<Fork> fork = obsHelper.newValue(Fork.EMPTY);

  /* Validator registry */

  private ObsValue<WriteList<ValidatorIndex, ValidatorRecord>> validatorRegistry =
      obsHelper.newValue(ObservableListImpl.create(ValidatorIndex::of));
  private ObsValue<WriteList<ValidatorIndex, Gwei>> balances =
      obsHelper.newValue(ObservableListImpl.create(ValidatorIndex::of));

  /* Randomness and committees */

  private ObsValue<WriteList<EpochNumber, Hash32>> latestRandaoMixes =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of, true));
  private ObsValue<ShardNumber> latestStartShard = obsHelper.newValue(ShardNumber.ZERO);

  /* Finality */

  private ObsValue<WriteList<Integer, PendingAttestation>> previousEpochAttestations =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));
  private ObsValue<WriteList<Integer, PendingAttestation>> currentEpochAttestations =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));
  private ObsValue<EpochNumber> previousJustifiedEpoch = obsHelper.newValue(EpochNumber.ZERO);
  private ObsValue<EpochNumber> currentJustifiedEpoch = obsHelper.newValue(EpochNumber.ZERO);
  private ObsValue<Hash32> previousJustifiedRoot = obsHelper.newValue(Hash32.ZERO);
  private ObsValue<Hash32> currentJustifiedRoot = obsHelper.newValue(Hash32.ZERO);
  private ObsValue<Bitfield64> justificationBitfield = obsHelper.newValue(Bitfield64.ZERO);
  private ObsValue<EpochNumber> finalizedEpoch = obsHelper.newValue(EpochNumber.ZERO);
  private ObsValue<Hash32> finalizedRoot = obsHelper.newValue(Hash32.ZERO);

  /* Recent state */

  private ObsValue<WriteList<ShardNumber, Crosslink>> previousCrosslinks =
      obsHelper.newValue(ObservableListImpl.create(ShardNumber::of, true));
  private ObsValue<WriteList<ShardNumber, Crosslink>> currentCrosslinks =
      obsHelper.newValue(ObservableListImpl.create(ShardNumber::of, true));
  private ObsValue<WriteList<SlotNumber, Hash32>> latestBlockRoots =
      obsHelper.newValue(ObservableListImpl.create(SlotNumber::of, true));
  private ObsValue<WriteList<SlotNumber, Hash32>> latestStateRoots =
      obsHelper.newValue(ObservableListImpl.create(SlotNumber::of, true));
  private ObsValue<WriteList<EpochNumber, Hash32>> latestActiveIndexRoots =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of, true));
  private ObsValue<WriteList<EpochNumber, Gwei>> latestSlashedBalances =
      obsHelper.newValue(ObservableListImpl.create(EpochNumber::of, true));
  private ObsValue<BeaconBlockHeader> latestBlockHeader = obsHelper.newValue(BeaconBlockHeader.EMPTY);
  private ObsValue<WriteList<Integer, Hash32>> historicalRoots =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));

  /* PoW receipt root */

  private ObsValue<Eth1Data> latestEth1Data = obsHelper.newValue(Eth1Data.EMPTY);
  private ObsValue<WriteList<Integer, Eth1Data>> eth1DataVotes =
      obsHelper.newValue(ObservableListImpl.create(Integer::valueOf));
  private ObsValue<UInt64> depositIndex = obsHelper.newValue(UInt64.ZERO);

  public BeaconStateImpl() {}

  BeaconStateImpl(BeaconState state) {
    slot.set(state.getSlot());
    genesisTime.set(state.getGenesisTime());
    fork.set(state.getFork());

    validatorRegistry.set(state.getValidatorRegistry().createMutableCopy());
    balances.set(state.getBalances().createMutableCopy());

    latestRandaoMixes.set(state.getLatestRandaoMixes().createMutableCopy());
    latestStartShard.set(state.getLatestStartShard());

    previousEpochAttestations.set(state.getPreviousEpochAttestations().createMutableCopy());
    currentEpochAttestations.set(state.getCurrentEpochAttestations().createMutableCopy());
    previousJustifiedEpoch.set(state.getPreviousJustifiedEpoch());
    currentJustifiedEpoch.set(state.getCurrentJustifiedEpoch());
    previousJustifiedRoot.set(state.getPreviousJustifiedRoot());
    currentJustifiedRoot.set(state.getCurrentJustifiedRoot());
    justificationBitfield.set(state.getJustificationBitfield());
    finalizedEpoch.set(state.getFinalizedEpoch());
    finalizedRoot.set(state.getFinalizedRoot());

    previousCrosslinks.set(state.getPreviousCrosslinks().createMutableCopy());
    currentCrosslinks.set(state.getCurrentCrosslinks().createMutableCopy());
    latestBlockRoots.set(state.getLatestBlockRoots().createMutableCopy());
    latestStateRoots.set(state.getLatestStateRoots().createMutableCopy());
    latestActiveIndexRoots.set(state.getLatestActiveIndexRoots().createMutableCopy());
    latestSlashedBalances.set(state.getLatestSlashedBalances().createMutableCopy());
    latestBlockHeader.set(state.getLatestBlockHeader());
    historicalRoots.set(state.getHistoricalRoots().createMutableCopy());

    latestEth1Data.set(state.getLatestEth1Data());
    eth1DataVotes.set(state.getEth1DataVotes().createMutableCopy());
    depositIndex.set(state.getDepositIndex());

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
  public WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry() {
    return validatorRegistry.get();
  }

  public void setValidatorRegistry(
      WriteList<ValidatorIndex, ValidatorRecord> validatorRegistry) {
    this.validatorRegistry.get().replaceAll(validatorRegistry);
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
  public WriteList<EpochNumber, Hash32> getLatestRandaoMixes() {
    return latestRandaoMixes.get();
  }

  public void setLatestRandaoMixes(
      WriteList<EpochNumber, Hash32> latestRandaoMixes) {
    this.latestRandaoMixes.get().replaceAll(latestRandaoMixes);
  }

  @Override
  public ShardNumber getLatestStartShard() {
    return latestStartShard.get();
  }

  @Override
  public void setLatestStartShard(ShardNumber latestStartShard) {
    this.latestStartShard.set(latestStartShard);
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
  public EpochNumber getPreviousJustifiedEpoch() {
    return previousJustifiedEpoch.get();
  }

  @Override
  public void setPreviousJustifiedEpoch(EpochNumber previousJustifiedEpoch) {
    this.previousJustifiedEpoch.set(previousJustifiedEpoch);
  }

  @Override
  public EpochNumber getCurrentJustifiedEpoch() {
    return currentJustifiedEpoch.get();
  }

  @Override
  public void setCurrentJustifiedEpoch(EpochNumber currentJustifiedEpoch) {
    this.currentJustifiedEpoch.set(currentJustifiedEpoch);
  }

  @Override
  public Hash32 getPreviousJustifiedRoot() {
    return previousJustifiedRoot.get();
  }

  @Override
  public void setPreviousJustifiedRoot(Hash32 previousJustifiedRoot) {
    this.previousJustifiedRoot.set(previousJustifiedRoot);
  }

  @Override
  public Hash32 getCurrentJustifiedRoot() {
    return currentJustifiedRoot.get();
  }

  @Override
  public void setCurrentJustifiedRoot(Hash32 currentJustifiedRoot) {
    this.currentJustifiedRoot.set(currentJustifiedRoot);
  }

  @Override
  public Bitfield64 getJustificationBitfield() {
    return justificationBitfield.get();
  }

  @Override
  public void setJustificationBitfield(Bitfield64 justificationBitfield) {
    this.justificationBitfield.set(justificationBitfield);
  }

  @Override
  public EpochNumber getFinalizedEpoch() {
    return finalizedEpoch.get();
  }

  @Override
  public void setFinalizedEpoch(EpochNumber finalizedEpoch) {
    this.finalizedEpoch.set(finalizedEpoch);
  }

  @Override
  public Hash32 getFinalizedRoot() {
    return finalizedRoot.get();
  }

  @Override
  public void setFinalizedRoot(Hash32 finalizedRoot) {
    this.finalizedRoot.set(finalizedRoot);
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
  public WriteList<SlotNumber, Hash32> getLatestBlockRoots() {
    return latestBlockRoots.get();
  }

  public void setLatestBlockRoots(
      WriteList<SlotNumber, Hash32> latestBlockRoots) {
    this.latestBlockRoots.get().replaceAll(latestBlockRoots);
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestStateRoots() {
    return latestStateRoots.get();
  }

  public void setLatestStateRoots(
      WriteList<SlotNumber, Hash32> latestStateRoots) {
    this.latestStateRoots.get().replaceAll(latestStateRoots);
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestActiveIndexRoots() {
    return latestActiveIndexRoots.get();
  }

  public void setLatestActiveIndexRoots(
      WriteList<EpochNumber, Hash32> latestActiveIndexRoots) {
    this.latestActiveIndexRoots.get().replaceAll(latestActiveIndexRoots);
  }

  @Override
  public WriteList<EpochNumber, Gwei> getLatestSlashedBalances() {
    return latestSlashedBalances.get();
  }

  public void setLatestSlashedBalances(
      WriteList<EpochNumber, Gwei> latestSlashedBalances) {
    this.latestSlashedBalances.get().replaceAll(latestSlashedBalances);
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
  public Eth1Data getLatestEth1Data() {
    return latestEth1Data.get();
  }

  @Override
  public void setLatestEth1Data(Eth1Data latestEth1Data) {
    this.latestEth1Data.set(latestEth1Data);
  }

  @Override
  public WriteList<Integer, Eth1Data> getEth1DataVotes() {
    return eth1DataVotes.get();
  }

  public void setEth1DataVotes(
      WriteList<Integer, Eth1Data> eth1DataVotes) {
    this.eth1DataVotes.get().replaceAll(eth1DataVotes);
  }

  @Override
  public UInt64 getDepositIndex() {
    return depositIndex.get();
  }

  @Override
  public void setDepositIndex(UInt64 depositIndex) {
    this.depositIndex.set(depositIndex);
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

package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface MutableBeaconState extends BeaconState {

  static MutableBeaconState createNew() {
    return new BeaconStateImpl();
  }

  void setSlot(SlotNumber slotNumber);

  void setGenesisTime(Time genesisTime);

  void setForkData(ForkData forkData);

  @Override
  WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry();

  @Override
  WriteList<ValidatorIndex, Gwei> getValidatorBalances();

  void setValidatorRegistryLatestChangeSlot(SlotNumber latestChangeSlot);

  void setValidatorRegistryExitCount(UInt64 exitCount);

  void setValidatorRegistryDeltaChainTip(Hash32 deltaChainTip);

  @Override
  WriteList<UInt64, Hash32> getLatestRandaoMixes();

  @Override
  WriteList<Integer, Hash32> getLatestVdfOutputs();

  void setPreviousEpochStartShard(ShardNumber previousEpochStartShard);

  void setCurrentEpochStartShard(ShardNumber currentEpochStartShard);

  void setPreviousEpochCalculationSlot(SlotNumber previousEpochCalculationSlot);

  void setCurrentEpochCalculationSlot(SlotNumber currentEpochCalculationSlot);

  void setPreviousEpochRandaoMix(Hash32 previousEpochRandaoMix);

  void setCurrentEpochRandaoMix(Hash32 currentEpochRandaoMix);

  @Override
  WriteList<Integer, CustodyChallenge> getCustodyChallenges();

  void setPreviousJustifiedSlot(SlotNumber previousJustifiedSlot);

  void setJustifiedSlot(SlotNumber justifiedSlot);

  void setJustificationBitfield(Bitfield justificationBitfield);

  void setFinalizedSlot(SlotNumber finalizedSlot);

  @Override
  WriteList<ShardNumber, CrosslinkRecord> getLatestCrosslinks();

  @Override
  WriteList<SlotNumber, Hash32> getLatestBlockRoots();

  @Override
  WriteList<EpochNumber, Gwei> getLatestPenalizedExitBalances();

  @Override
  WriteList<Integer, PendingAttestationRecord> getLatestAttestations();

  @Override
  WriteList<Integer, Hash32> getBatchedBlockRoots();

  void setLatestEth1Data(Eth1Data latestEth1Data);

  @Override
  WriteList<Integer, Eth1DataVote> getEth1DataVotes();

  BeaconState createImmutable();

  /*
  void setSlot(UInt64 slot);

  void setGenesisTime(UInt64 genesisTime);

  void setForkData(ForkData forkData);

  void setValidatorRegistry(List<ValidatorRecord> validatorRegistry);

  void setValidatorBalances(List<UInt64> validatorBalances);

  void setValidatorRegistryLatestChangeSlot(UInt64 validatorRegistryLatestChangeSlot);

  void setValidatorRegistryExitCount(UInt64 validatorRegistryExitCount);

  void setValidatorRegistryDeltaChainTip(Hash32 validatorRegistryDeltaChainTip);

  void setLatestRandaoMixes(List<Hash32> latestRandaoMixes);

  void setLatestVdfOutputs(List<Hash32> latestVdfOutputs);

  void setPreviousEpochStartShard(UInt64 previousEpochStartShard);

  void setCurrentEpochStartShard(UInt64 currentEpochStartShard);

  void setPreviousEpochCalculationSlot(UInt64 previousEpochCalculationSlot);

  void setCurrentEpochCalculationSlot(UInt64 currentEpochCalculationSlot);

  void setPreviousEpochRandaoMix(Hash32 previousEpochRandaoMix);

  void setCurrentEpochRandaoMix(Hash32 currentEpochRandaoMix);

  void setCustodyChallenges(List<CustodyChallenge> custodyChallenges);

  void setPreviousJustifiedSlot(UInt64 previousJustifiedSlot);

  void setJustifiedSlot(UInt64 justifiedSlot);

  void setJustificationBitfield(UInt64 justificationBitfield);

  void setFinalizedSlot(UInt64 finalizedSlot);

  void setLatestCrosslinks(List<CrosslinkRecord> latestCrosslinks);

  void setLatestBlockRoots(List<Hash32> latestBlockRoots);

  void setLatestPenalizedExitBalances(List<UInt64> latestPenalizedExitBalances);

  void setLatestAttestations(List<PendingAttestationRecord> latestAttestations);

  void setBatchedBlockRoots(List<Hash32> batchedBlockRoots);

  void setLatestEth1Data(Eth1Data latestEth1Data);

  void setEth1DataVotes(List<Eth1DataVote> eth1DataVotes);

  BeaconState validate();

  default MutableBeaconState withSlot(UInt64 slot) {
    setSlot(slot);
    return this;
  }

  default MutableBeaconState withGenesisTime(UInt64 genesisTime) {
    setGenesisTime(genesisTime);
    return this;
  }

  default MutableBeaconState withForkData(ForkData forkData) {
    setForkData(forkData);
    return this;
  }

  default MutableBeaconState withValidatorRegistry(List<ValidatorRecord> validatorRegistry) {
    setValidatorRegistry(validatorRegistry);
    return this;
  }

  default MutableBeaconState withNewValidatorRecord(ValidatorRecord newValidatorRecord) {
    ArrayList<ValidatorRecord> newRegistry = new ArrayList<>(getValidatorRegistry());
    newRegistry.add(newValidatorRecord);
    return withValidatorRegistry(newRegistry);
  }

  default MutableBeaconState withValidatorRecord(
      int idx, Consumer<ValidatorRecord.Builder> validatorUpdater) {
    ArrayList<ValidatorRecord> newRegistry = new ArrayList<>(getValidatorRegistry());
    ValidatorRecord.Builder builder =
        ValidatorRecord.Builder.fromRecord(getValidatorRegistry().get(idx));
    validatorUpdater.accept(builder);
    newRegistry.set(idx, builder.build());
    return withValidatorRegistry(newRegistry);
  }

  default MutableBeaconState withValidatorBalances(List<UInt64> validatorBalances) {
    setValidatorBalances(validatorBalances);
    return this;
  }

  default MutableBeaconState withNewValidatorBalance(UInt64 balance) {
    ArrayList<UInt64> validatorBalances = new ArrayList<>(getValidatorBalances());
    validatorBalances.add(balance);
    return withValidatorBalances(validatorBalances);
  }

  default MutableBeaconState withValidatorBalance(
      UInt24 idx, Function<UInt64, UInt64> validatorBalanceUpdater) {
    ArrayList<UInt64> validatorBalances = new ArrayList<>(getValidatorBalances());
    validatorBalances.set(
        idx.getValue(), validatorBalanceUpdater.apply(validatorBalances.get(idx.getValue())));
    return withValidatorBalances(validatorBalances);
  }

  default MutableBeaconState withValidatorRegistryLatestChangeSlot(
      UInt64 validatorRegistryLatestChangeSlot) {
    setValidatorRegistryLatestChangeSlot(validatorRegistryLatestChangeSlot);
    return this;
  }

  default MutableBeaconState withValidatorRegistryExitCount(UInt64 validatorRegistryExitCount) {
    setValidatorRegistryExitCount(validatorRegistryExitCount);
    return this;
  }

  default MutableBeaconState withValidatorRegistryDeltaChainTip(
      Hash32 validatorRegistryDeltaChainTip) {
    setValidatorRegistryDeltaChainTip(validatorRegistryDeltaChainTip);
    return this;
  }

  default MutableBeaconState withLatestRandaoMixes(List<Hash32> latestRandaoMixes) {
    setLatestRandaoMixes(latestRandaoMixes);
    return this;
  }

  default MutableBeaconState withLatestVdfOutputs(List<Hash32> latestVdfOutputs) {
    setLatestVdfOutputs(latestVdfOutputs);
    return this;
  }

  default MutableBeaconState withPreviousEpochStartShard(UInt64 previousEpochStartShard) {
    setPreviousEpochStartShard(previousEpochStartShard);
    return this;
  }

  default MutableBeaconState withCurrentEpochStartShard(UInt64 currentEpochStartShard) {
    setCurrentEpochStartShard(currentEpochStartShard);
    return this;
  }

  default MutableBeaconState withPreviousEpochCalculationSlot(UInt64 previousEpochCalculationSlot) {
    setPreviousEpochCalculationSlot(previousEpochCalculationSlot);
    return this;
  }

  default MutableBeaconState withCurrentEpochCalculationSlot(UInt64 currentEpochCalculationSlot) {
    setCurrentEpochCalculationSlot(currentEpochCalculationSlot);
    return this;
  }

  default MutableBeaconState withPreviousEpochRandaoMix(Hash32 previousEpochRandaoMix) {
    setPreviousEpochRandaoMix(previousEpochRandaoMix);
    return this;
  }

  default MutableBeaconState withCurrentEpochRandaoMix(Hash32 currentEpochRandaoMix) {
    setCurrentEpochRandaoMix(currentEpochRandaoMix);
    return this;
  }

  default MutableBeaconState withCustodyChallenges(List<CustodyChallenge> custodyChallenges) {
    setCustodyChallenges(custodyChallenges);
    return this;
  }

  default MutableBeaconState withPreviousJustifiedSlot(UInt64 previousJustifiedSlot) {
    setPreviousJustifiedSlot(previousJustifiedSlot);
    return this;
  }

  default MutableBeaconState withJustifiedSlot(UInt64 justifiedSlot) {
    setJustifiedSlot(justifiedSlot);
    return this;
  }

  default MutableBeaconState withJustificationBitfield(UInt64 justificationBitfield) {
    setJustificationBitfield(justificationBitfield);
    return this;
  }

  default MutableBeaconState withFinalizedSlot(UInt64 finalizedSlot) {
    setFinalizedSlot(finalizedSlot);
    return this;
  }

  default MutableBeaconState withLatestCrosslinks(List<CrosslinkRecord> latestCrosslinks) {
    setLatestCrosslinks(latestCrosslinks);
    return this;
  }

  default MutableBeaconState withLatestBlockRoots(List<Hash32> latestBlockRoots) {
    setLatestBlockRoots(latestBlockRoots);
    return this;
  }

  default MutableBeaconState withLatestPenalizedExitBalances(
      List<UInt64> latestPenalizedExitBalances) {
    setLatestPenalizedExitBalances(latestPenalizedExitBalances);
    return this;
  }

  default MutableBeaconState withLatestPenalizedExitBalance(
      int idx, Function<UInt64, UInt64> balanceUpdater) {
    ArrayList<UInt64> latestPenalizedExitBalances =
        new ArrayList<>(getLatestPenalizedExitBalances());
    latestPenalizedExitBalances.set(
        idx, balanceUpdater.apply(latestPenalizedExitBalances.get(idx)));
    return withLatestPenalizedExitBalances(latestPenalizedExitBalances);
  }

  default MutableBeaconState withLatestAttestations(
      List<PendingAttestationRecord> latestAttestations) {
    setLatestAttestations(latestAttestations);
    return this;
  }

  default MutableBeaconState withNewLatestAttestation(PendingAttestationRecord latestAttestation) {
    ArrayList<PendingAttestationRecord> newRecords = new ArrayList<>(getLatestAttestations());
    newRecords.add(latestAttestation);
    return withLatestAttestations(newRecords);
  }

  default MutableBeaconState withBatchedBlockRoots(List<Hash32> batchedBlockRoots) {
    setBatchedBlockRoots(batchedBlockRoots);
    return this;
  }

  default MutableBeaconState withLatestEth1Data(Eth1Data latestEth1Data) {
    setLatestEth1Data(latestEth1Data);
    return this;
  }

  default MutableBeaconState withEth1DataVotes(List<Eth1DataVote> eth1DataVotes) {
    setEth1DataVotes(eth1DataVotes);
    return this;
  }

  default MutableBeaconState withEth1DataVote(
      int idx, Function<Eth1DataVote, Eth1DataVote> voteUpdater) {
    ArrayList<Eth1DataVote> newVotes = new ArrayList<>(getEth1DataVotes());
    newVotes.set(idx, voteUpdater.apply(newVotes.get(idx)));
    return withEth1DataVotes(newVotes);
  }

  default MutableBeaconState withNewEth1DataVote(Eth1DataVote newVote) {
    ArrayList<Eth1DataVote> newVotes = new ArrayList<>(getEth1DataVotes());
    newVotes.add(newVote);
    return withEth1DataVotes(newVotes);
  }
  */
}

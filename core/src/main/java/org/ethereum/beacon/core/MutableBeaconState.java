package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.state.*;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface MutableBeaconState extends BeaconState {

  static MutableBeaconState createNew() {
    return new BeaconStateImpl();
  }

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

  void setLatestDepositRoot(Hash32 latestDepositRoot);

  void setDepositRootVotes(List<DepositRootVote> depositRootVotes);

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

  default MutableBeaconState withValidatorRecord(int idx,
      Consumer<ValidatorRecord.Builder> validatorUpdater) {
    ArrayList<ValidatorRecord> newRegistry = new ArrayList<>(getValidatorRegistry());
    ValidatorRecord.Builder builder = ValidatorRecord.Builder
        .fromRecord(getValidatorRegistry().get(idx));
    validatorUpdater.accept(builder);
    newRegistry.set(idx, builder.build());
    return withValidatorRegistry(newRegistry);
  }

  default MutableBeaconState withValidatorBalances(List<UInt64> validatorBalances) {
    setValidatorBalances(validatorBalances);
    return this;
  }

  default MutableBeaconState withValidatorBalance(int idx,
      Function<UInt64, UInt64> validatorBalanceUpdater) {
    ArrayList<UInt64> validatorBalances = new ArrayList<>(getValidatorBalances());
    validatorBalances.set(idx, validatorBalanceUpdater.apply(validatorBalances.get(idx)));
    return withValidatorBalances(validatorBalances);
  }

  default MutableBeaconState withValidatorRegistryLatestChangeSlot(UInt64 validatorRegistryLatestChangeSlot) {
    setValidatorRegistryLatestChangeSlot(validatorRegistryLatestChangeSlot);
    return this;
  }

  default MutableBeaconState withValidatorRegistryExitCount(UInt64 validatorRegistryExitCount) {
    setValidatorRegistryExitCount(validatorRegistryExitCount);
    return this;
  }

  default MutableBeaconState withValidatorRegistryDeltaChainTip(Hash32 validatorRegistryDeltaChainTip) {
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

  default MutableBeaconState withLatestPenalizedExitBalances(List<UInt64> latestPenalizedExitBalances) {
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

  default MutableBeaconState withLatestAttestations(List<PendingAttestationRecord> latestAttestations) {
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

  default MutableBeaconState withLatestDepositRoot(Hash32 latestDepositRoot) {
    setLatestDepositRoot(latestDepositRoot);
    return this;
  }

  default MutableBeaconState withDepositRootVotes(List<DepositRootVote> depositRootVotes) {
    setDepositRootVotes(depositRootVotes);
    return this;
  }

  default MutableBeaconState withDepositRootVote(
      int idx, Function<DepositRootVote, DepositRootVote> voteUpdater) {
    ArrayList<DepositRootVote> newVotes = new ArrayList<>(getDepositRootVotes());
    newVotes.set(idx, voteUpdater.apply(newVotes.get(idx)));
    return withDepositRootVotes(newVotes);
  }

  default MutableBeaconState withNewDepositRootVote(DepositRootVote newVote) {
    ArrayList<DepositRootVote> newVotes = new ArrayList<>(getDepositRootVotes());
    newVotes.add(newVote);
    return withDepositRootVotes(newVotes);
  }
}

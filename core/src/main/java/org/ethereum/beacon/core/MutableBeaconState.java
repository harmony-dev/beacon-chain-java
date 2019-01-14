package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.state.*;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;

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

  void setShardCommitteesAtSlots(ShardCommittee[][] shardCommitteesAtSlots);

  void setShardCommitteesAtSlots(List<List<ShardCommittee>> shardCommitteesAtSlots);

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

  default MutableBeaconState withValidatorBalances(List<UInt64> validatorBalances) {
    setValidatorBalances(validatorBalances);
    return this;
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

  default MutableBeaconState withShardCommitteesAtSlots(ShardCommittee[][] shardCommitteesAtSlots) {
    setShardCommitteesAtSlots(shardCommitteesAtSlots);
    return this;
  }

  default MutableBeaconState withShardCommitteesAtSlots(List<List<ShardCommittee>> shardCommitteesAtSlots) {
    setShardCommitteesAtSlots(shardCommitteesAtSlots);
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

  default MutableBeaconState withLatestAttestations(List<PendingAttestationRecord> latestAttestations) {
    setLatestAttestations(latestAttestations);
    return this;
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
}

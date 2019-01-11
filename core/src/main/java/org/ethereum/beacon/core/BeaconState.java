package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.state.*;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;

public interface BeaconState extends Hashable<Hash32> {

  UInt64 getSlot();

  UInt64 getGenesisTime();

  ForkData getForkData();

  List<ValidatorRecord> getValidatorRegistry();

  List<UInt64> getValidatorBalances();

  UInt64 getValidatorRegistryLatestChangeSlot();

  UInt64 getValidatorRegistryExitCount();

  Hash32 getValidatorRegistryDeltaChainTip();

  List<Hash32> getLatestRandaoMixes();

  List<Hash32> getLatestVdfOutputs();

  List<List<ShardCommittee>> getShardCommitteesAtSlots();

  List<CustodyChallenge> getCustodyChallenges();

  UInt64 getPreviousJustifiedSlot();

  UInt64 getJustifiedSlot();

  UInt64 getJustificationBitfield();

  UInt64 getFinalizedSlot();

  List<CrosslinkRecord> getLatestCrosslinks();

  List<Hash32> getLatestBlockRoots();

  List<UInt64> getLatestPenalizedExitBalances();

  List<PendingAttestationRecord> getLatestAttestations();

  List<Hash32> getBatchedBlockRoots();

  Hash32 getLatestDepositRoot();

  List<DepositRootVote> getDepositRootVotes();
}

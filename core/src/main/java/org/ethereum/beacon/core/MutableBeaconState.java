package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.state.*;
import org.ethereum.beacon.core.state.ValidatorRecord.Builder;
import org.ethereum.beacon.core.types.GWei;
import org.ethereum.beacon.core.types.Shard;
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

  @Override
  WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry();

  @Override
  WriteList<ValidatorIndex, GWei> getValidatorBalances();

  @Override
  WriteList<Integer, Hash32> getLatestRandaoMixes();

  @Override
  WriteList<Integer, Hash32> getLatestVdfOutputs();

  @Override
  WriteList<Integer, CustodyChallenge> getCustodyChallenges();

  @Override
  WriteList<Shard, CrosslinkRecord> getLatestCrosslinks();

  @Override
  WriteList<Integer, Hash32> getLatestBlockRoots();

  @Override
  WriteList<Integer, GWei> getLatestPenalizedExitBalances();

  @Override
  WriteList<Integer, PendingAttestationRecord> getLatestAttestations();

  @Override
  WriteList<Integer, Hash32> getBatchedBlockRoots();

  @Override
  WriteList<Integer, DepositRootVote> getDepositRootVotes();

  void setSlot(UInt64 slot);

  void setGenesisTime(UInt64 genesisTime);

  void setForkData(ForkData forkData);

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

  void setPreviousJustifiedSlot(UInt64 previousJustifiedSlot);

  void setJustifiedSlot(UInt64 justifiedSlot);

  void setJustificationBitfield(UInt64 justificationBitfield);

  void setFinalizedSlot(UInt64 finalizedSlot);

  void setLatestDepositRoot(Hash32 latestDepositRoot);

  BeaconState createImmutable();

}

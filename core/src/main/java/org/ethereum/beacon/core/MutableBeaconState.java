package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;

public interface MutableBeaconState extends BeaconState {

  void setSlot(SlotNumber slotNumber);

  void setGenesisTime(Time genesisTime);

  void setForkData(ForkData forkData);

  @Override
  WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry();

  @Override
  WriteList<ValidatorIndex, Gwei> getValidatorBalances();

  void setValidatorRegistryUpdateEpoch(EpochNumber validatorRegistryUpdateEpoch);

  @Override
  WriteList<EpochNumber, Hash32> getLatestRandaoMixes();

  void setPreviousShufflingStartShard(ShardNumber previousShufflingStartShard);

  void setCurrentShufflingStartShard(ShardNumber currentShufflingStartShard);

  void setPreviousShufflingEpoch(EpochNumber previousShufflingEpoch);

  void setCurrentShufflingEpoch(EpochNumber currentShufflingEpoch);

  void setPreviousShufflingSeed(Hash32 previousEpochRandaoMix);

  void setCurrentShufflingSeed(Hash32 currentEpochRandaoMix);

  void setPreviousJustifiedEpoch(EpochNumber previousJustifiedEpoch);

  void setJustifiedEpoch(EpochNumber justifiedSlot);

  void setJustificationBitfield(Bitfield64 justificationBitfield);

  void setFinalizedEpoch(EpochNumber finalizedEpoch);

  @Override
  WriteList<ShardNumber, Crosslink> getLatestCrosslinks();

  @Override
  WriteList<SlotNumber, Hash32> getLatestBlockRoots();

  @Override
  WriteList<EpochNumber, Hash32> getLatestActiveIndexRoots();

  @Override
  WriteList<EpochNumber, Gwei> getLatestSlashedBalances();

  @Override
  WriteList<Integer, PendingAttestationRecord> getLatestAttestations();

  @Override
  WriteList<Integer, Hash32> getBatchedBlockRoots();

  void setLatestEth1Data(Eth1Data latestEth1Data);

  @Override
  WriteList<Integer, Eth1DataVote> getEth1DataVotes();

  void setDepositIndex(UInt64 depositIndex);

  BeaconState createImmutable();
}

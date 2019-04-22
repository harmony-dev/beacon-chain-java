package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.collections.WriteVector;
import tech.pegasys.artemis.util.uint.UInt64;

public interface MutableBeaconState extends BeaconState {

  void setSlot(SlotNumber slotNumber);

  void setGenesisTime(Time genesisTime);

  void setFork(Fork fork);

  @Override
  WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry();

  @Override
  WriteList<ValidatorIndex, Gwei> getValidatorBalances();

  void setValidatorRegistryUpdateEpoch(EpochNumber validatorRegistryUpdateEpoch);

  @Override
  WriteVector<EpochNumber, Hash32> getLatestRandaoMixes();

  void setPreviousShufflingStartShard(ShardNumber previousShufflingStartShard);

  void setCurrentShufflingStartShard(ShardNumber currentShufflingStartShard);

  void setPreviousShufflingEpoch(EpochNumber previousShufflingEpoch);

  void setCurrentShufflingEpoch(EpochNumber currentShufflingEpoch);

  void setPreviousShufflingSeed(Hash32 previousEpochRandaoMix);

  void setCurrentShufflingSeed(Hash32 currentEpochRandaoMix);

  void setPreviousJustifiedEpoch(EpochNumber previousJustifiedEpoch);

  void setCurrentJustifiedEpoch(EpochNumber currentJustifiedSlot);

  void setPreviousJustifiedRoot(Hash32 previousJustifiedRoot);

  void setCurrentJustifiedRoot(Hash32 currentJustifiedRoot);

  void setJustificationBitfield(Bitfield64 justificationBitfield);

  void setFinalizedEpoch(EpochNumber finalizedEpoch);

  void setFinalizedRoot(Hash32 finalizedRoot);

  void setLatestBlockHeader(BeaconBlockHeader latestBlockHeader);

  @Override
  WriteList<ShardNumber, Crosslink> getPreviousCrosslinks();

  @Override
  WriteList<ShardNumber, Crosslink> getCurrentCrosslinks();

  @Override
  WriteVector<SlotNumber, Hash32> getLatestBlockRoots();

  @Override
  WriteVector<SlotNumber, Hash32> getLatestStateRoots();

  @Override
  WriteVector<EpochNumber, Hash32> getLatestActiveIndexRoots();

  @Override
  WriteVector<EpochNumber, Gwei> getLatestSlashedBalances();

  @Override
  WriteList<Integer, PendingAttestation> getPreviousEpochAttestations();

  @Override
  WriteList<Integer, PendingAttestation> getCurrentEpochAttestations();

  @Override
  WriteList<Integer, Hash32> getHistoricalRoots();

  void setLatestEth1Data(Eth1Data latestEth1Data);

  @Override
  WriteList<Integer, Eth1DataVote> getEth1DataVotes();

  void setDepositIndex(UInt64 depositIndex);

  BeaconState createImmutable();
}

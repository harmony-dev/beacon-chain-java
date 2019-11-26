package org.ethereum.beacon.core;

import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import tech.pegasys.artemis.util.collections.Bitvector;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.collections.WriteVector;
import tech.pegasys.artemis.util.uint.UInt64;

public interface MutableBeaconState extends BeaconState {

  void setSlot(SlotNumber slotNumber);

  void setGenesisTime(Time genesisTime);

  void setFork(Fork fork);

  @Override
  WriteList<ValidatorIndex, ValidatorRecord> getValidators();

  @Override
  WriteList<ValidatorIndex, Gwei> getBalances();

  @Override
  WriteVector<EpochNumber, Hash32> getRandaoMixes();

  void setRandaoMixes(WriteList<EpochNumber, Hash32> randaoMixes);

  void setJustificationBits(Bitvector justificationBits);

  void setLatestBlockHeader(BeaconBlockHeader latestBlockHeader);

  void setPreviousJustifiedCheckpoint(Checkpoint previousJustifiedCheckpoint);

  void setCurrentJustifiedCheckpoint(Checkpoint currentJustifiedCheckpoint);

  void setFinalizedCheckpoint(Checkpoint finalizedCheckpoint);

  @Override
  Bitvector getJustificationBits();

  @Override
  WriteVector<SlotNumber, Hash32> getBlockRoots();

  @Override
  WriteVector<SlotNumber, Hash32> getStateRoots();

  @Override
  WriteVector<EpochNumber, Gwei> getSlashings();

  @Override
  WriteList<Integer, PendingAttestation> getPreviousEpochAttestations();

  @Override
  WriteList<Integer, PendingAttestation> getCurrentEpochAttestations();

  @Override
  WriteList<Integer, Hash32> getHistoricalRoots();

  void setEth1Data(Eth1Data latestEth1Data);

  @Override
  WriteList<Integer, Eth1Data> getEth1DataVotes();

  void setEth1DataVotes(WriteList<Integer, Eth1Data> eth1DataVotes);

  void setEth1DepositIndex(UInt64 depositIndex);

  BeaconState createImmutable();
}

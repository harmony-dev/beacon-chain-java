package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.Serializer;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;

import java.util.ArrayList;
import java.util.List;

@SSZSerializable
public class ImmutableBeaconStateImpl implements BeaconState {

  /* Misc */
  @SSZ private final SlotNumber slot;
  @SSZ private final Time genesisTime;
  @SSZ private final ForkData forkData;

  /* Validator registry */

  @SSZ private final List<ValidatorRecord> validatorRegistryList;
  @SSZ private final List<Gwei> validatorBalancesList;
  @SSZ private final EpochNumber validatorRegistryUpdateEpoch;

  /* Randomness and committees */

  @SSZ private final List<Hash32> latestRandaoMixesList;
  @SSZ private final ShardNumber previousEpochStartShard;
  @SSZ private final ShardNumber currentEpochStartShard;
  @SSZ private final EpochNumber previousCalculationEpoch;
  @SSZ private final EpochNumber currentCalculationEpoch;
  @SSZ private final Hash32 previousEpochSeed;
  @SSZ private final Hash32 currentEpochSeed;

  /* Finality */

  @SSZ private final EpochNumber previousJustifiedEpoch;
  @SSZ private final EpochNumber justifiedEpoch;
  @SSZ private final Bitfield64 justificationBitfield;
  @SSZ private final EpochNumber finalizedEpoch;

  /* Recent state */

  @SSZ private final List<CrosslinkRecord> latestCrosslinksList;
  @SSZ private final List<Hash32> latestBlockRootsList;
  @SSZ private final List<Hash32> latestIndexRootsList;
  @SSZ private final List<Gwei> latestPenalizedBalancesList;
  @SSZ private final List<PendingAttestationRecord> latestAttestationsList;
  @SSZ private final List<Hash32> batchedBlockRootsList;

  /* PoW receipt root */

  @SSZ private final Eth1Data latestEth1Data;
  @SSZ private final List<Eth1DataVote> eth1DataVotesList;

  public ImmutableBeaconStateImpl(BeaconState state) {
    slot = state.getSlot();
    genesisTime = state.getGenesisTime();
    forkData = state.getForkData();

    validatorRegistryList = state.getValidatorRegistry().listCopy();
    validatorBalancesList = state.getValidatorBalances().listCopy();
    validatorRegistryUpdateEpoch = state.getValidatorRegistryUpdateEpoch();

    latestRandaoMixesList = state.getLatestRandaoMixes().listCopy();
    previousEpochStartShard = state.getPreviousEpochStartShard();
    currentEpochStartShard = state.getCurrentEpochStartShard();
    previousCalculationEpoch = state.getPreviousCalculationEpoch();
    currentCalculationEpoch = state.getCurrentCalculationEpoch();
    previousEpochSeed = state.getPreviousEpochSeed();
    currentEpochSeed = state.getCurrentEpochSeed();

    previousJustifiedEpoch = state.getPreviousJustifiedEpoch();
    justifiedEpoch = state.getJustifiedEpoch();
    justificationBitfield = state.getJustificationBitfield();
    finalizedEpoch = state.getFinalizedEpoch();

    latestCrosslinksList = state.getLatestCrosslinks().listCopy();
    latestBlockRootsList = state.getLatestBlockRoots().listCopy();
    latestIndexRootsList = state.getLatestIndexRoots().listCopy();
    latestPenalizedBalancesList = state.getLatestPenalizedBalances().listCopy();
    latestAttestationsList = state.getLatestAttestations().listCopy();
    batchedBlockRootsList = state.getBatchedBlockRoots().listCopy();

    latestEth1Data = state.getLatestEth1Data();
    eth1DataVotesList = state.getEth1DataVotes().listCopy();
  }

  /** ******* List Getters for serialization ********* */
  @Override
  public SlotNumber getSlot() {
    return slot;
  }

  @Override
  public Time getGenesisTime() {
    return genesisTime;
  }

  @Override
  public ForkData getForkData() {
    return forkData;
  }

  public List<ValidatorRecord> getValidatorRegistryList() {
    return validatorRegistryList;
  }

  public List<Gwei> getValidatorBalancesList() {
    return validatorBalancesList;
  }

  @Override
  public EpochNumber getValidatorRegistryUpdateEpoch() {
    return validatorRegistryUpdateEpoch;
  }

  public List<Hash32> getLatestRandaoMixesList() {
    return new ArrayList<>(latestRandaoMixesList);
  }

  @Override
  public ShardNumber getPreviousEpochStartShard() {
    return previousEpochStartShard;
  }

  @Override
  public ShardNumber getCurrentEpochStartShard() {
    return currentEpochStartShard;
  }

  @Override
  public EpochNumber getPreviousCalculationEpoch() {
    return previousCalculationEpoch;
  }

  @Override
  public EpochNumber getCurrentCalculationEpoch() {
    return currentCalculationEpoch;
  }

  @Override
  public Hash32 getPreviousEpochSeed() {
    return previousEpochSeed;
  }

  @Override
  public Hash32 getCurrentEpochSeed() {
    return currentEpochSeed;
  }

  @Override
  public EpochNumber getPreviousJustifiedEpoch() {
    return previousJustifiedEpoch;
  }

  @Override
  public EpochNumber getJustifiedEpoch() {
    return justifiedEpoch;
  }

  @Override
  public Bitfield64 getJustificationBitfield() {
    return justificationBitfield;
  }

  @Override
  public EpochNumber getFinalizedEpoch() {
    return finalizedEpoch;
  }

  public List<CrosslinkRecord> getLatestCrosslinksList() {
    return new ArrayList<>(latestCrosslinksList);
  }

  public List<Hash32> getLatestBlockRootsList() {
    return new ArrayList<>(latestBlockRootsList);
  }

  public List<Hash32> getLatestIndexRootsList() {
    return new ArrayList<>(latestIndexRootsList);
  }

  public List<Gwei> getLatestPenalizedBalancesList() {
    return new ArrayList<>(latestPenalizedBalancesList);
  }

  public List<PendingAttestationRecord> getLatestAttestationsList() {
    return new ArrayList<>(latestAttestationsList);
  }

  public List<Hash32> getBatchedBlockRootsList() {
    return new ArrayList<>(batchedBlockRootsList);
  }

  @Override
  public Eth1Data getLatestEth1Data() {
    return latestEth1Data;
  }

  public List<Eth1DataVote> getEth1DataVotesList() {
    return eth1DataVotesList;
  }

  @Override
  public ReadList<ValidatorIndex, ValidatorRecord> getValidatorRegistry() {
    return ReadList.wrap(validatorRegistryList, ValidatorIndex::of);
  }

  @Override
  public ReadList<ValidatorIndex, Gwei> getValidatorBalances() {
    return ReadList.wrap(validatorBalancesList, ValidatorIndex::of);
  }

  @Override
  public ReadList<EpochNumber, Hash32> getLatestRandaoMixes() {
    return ReadList.wrap(latestRandaoMixesList, EpochNumber::of);
  }

  @Override
  public ReadList<ShardNumber, CrosslinkRecord> getLatestCrosslinks() {
    return ReadList.wrap(latestCrosslinksList, ShardNumber::of);
  }

  @Override
  public ReadList<SlotNumber, Hash32> getLatestBlockRoots() {
    return ReadList.wrap(latestBlockRootsList, SlotNumber::of);
  }

  @Override
  public ReadList<EpochNumber, Hash32> getLatestIndexRoots() {
    return ReadList.wrap(latestIndexRootsList, EpochNumber::of);
  }

  @Override
  public ReadList<EpochNumber, Gwei> getLatestPenalizedBalances() {
    return ReadList.wrap(latestPenalizedBalancesList, EpochNumber::of);
  }

  @Override
  public ReadList<Integer, PendingAttestationRecord> getLatestAttestations() {
    return ReadList.wrap(latestAttestationsList, Integer::valueOf);
  }

  @Override
  public ReadList<Integer, Hash32> getBatchedBlockRoots() {
    return ReadList.wrap(batchedBlockRootsList, Integer::valueOf);
  }

  @Override
  public ReadList<Integer, Eth1DataVote> getEth1DataVotes() {
    return ReadList.wrap(eth1DataVotesList, Integer::valueOf);
  }

  @Override
  public MutableBeaconState createMutableCopy() {
    return new BeaconStateImpl(this);
  }

  @Override
  public boolean equals(Object o) {
    Serializer serializer = Serializer.annotationSerializer();
    return serializer.encode2(this).equals(serializer.encode2(o));
  }

  @Override
  public String toString() {
    return toStringShort(null);
  }
}

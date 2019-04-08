package org.ethereum.beacon.core.state;

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
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  /* Misc */

  private SlotNumber slot = SlotNumber.ZERO;
  private Time genesisTime = Time.ZERO;
  private ForkData forkData = ForkData.EMPTY;

  /* Validator registry */

  private WriteList<ValidatorIndex, ValidatorRecord> validatorRegistry =
      WriteList.create(ValidatorIndex::of);
  private WriteList<ValidatorIndex, Gwei> validatorBalances =
      WriteList.create(ValidatorIndex::of);
  private EpochNumber validatorRegistryUpdateEpoch = EpochNumber.ZERO;

  /* Randomness and committees */

  private WriteList<EpochNumber, Hash32> latestRandaoMixes =
      WriteList.create(EpochNumber::of);
  private ShardNumber previousShufflingStartShard = ShardNumber.ZERO;
  private ShardNumber currentShufflingStartShard = ShardNumber.ZERO;
  private EpochNumber previousShufflingEpoch = EpochNumber.ZERO;
  private EpochNumber currentShufflingEpoch = EpochNumber.ZERO;
  private Hash32 previousShufflingSeed = Hash32.ZERO;
  private Hash32 currentShufflingSeed = Hash32.ZERO;

  /* Finality */

  private EpochNumber previousJustifiedEpoch = EpochNumber.ZERO;
  private EpochNumber justifiedEpoch = EpochNumber.ZERO;
  private Bitfield64 justificationBitfield = Bitfield64.ZERO;
  private EpochNumber finalizedEpoch = EpochNumber.ZERO;

  /* Recent state */

  private WriteList<ShardNumber, Crosslink> latestCrosslinks =
      WriteList.create(ShardNumber::of);
  private WriteList<SlotNumber, Hash32> latestBlockRoots =
      WriteList.create(SlotNumber::of);
  private WriteList<EpochNumber, Hash32> latestActiveIndexRoots =
      WriteList.create(EpochNumber::of);
  private WriteList<EpochNumber, Gwei> latestSlashedBalances =
      WriteList.create(EpochNumber::of);
  private WriteList<Integer, PendingAttestationRecord> latestAttestations =
      WriteList.create(Integer::valueOf);
  private WriteList<Integer, Hash32> batchedBlockRoots =
      WriteList.create(Integer::valueOf);

  /* PoW receipt root */

  private Eth1Data latestEth1Data = Eth1Data.EMPTY;
  private WriteList<Integer, Eth1DataVote> eth1DataVotes =
      WriteList.create(Integer::valueOf);
  private UInt64 depositIndex = UInt64.ZERO;

  public BeaconStateImpl() {}

  BeaconStateImpl(BeaconState state) {
        slot = state.getSlot();
        genesisTime = state.getGenesisTime();
        forkData = state.getForkData();

        validatorRegistry = state.getValidatorRegistry().createMutableCopy();
        validatorBalances = state.getValidatorBalances().createMutableCopy();
        validatorRegistryUpdateEpoch = state.getValidatorRegistryUpdateEpoch();

        latestRandaoMixes = state.getLatestRandaoMixes().createMutableCopy();
        previousShufflingStartShard = state.getPreviousShufflingStartShard();
        currentShufflingStartShard = state.getCurrentShufflingStartShard();
        previousShufflingEpoch = state.getPreviousShufflingEpoch();
        currentShufflingEpoch = state.getCurrentShufflingEpoch();
        previousShufflingSeed = state.getPreviousShufflingSeed();
        currentShufflingSeed = state.getCurrentShufflingSeed();

        previousJustifiedEpoch = state.getPreviousJustifiedEpoch();
        justifiedEpoch = state.getJustifiedEpoch();
        justificationBitfield = state.getJustificationBitfield();
        finalizedEpoch = state.getFinalizedEpoch();

        latestCrosslinks = state.getLatestCrosslinks().createMutableCopy();
        latestBlockRoots = state.getLatestBlockRoots().createMutableCopy();
        latestActiveIndexRoots = state.getLatestActiveIndexRoots().createMutableCopy();
        latestSlashedBalances = state.getLatestSlashedBalances().createMutableCopy();
        latestAttestations = state.getLatestAttestations().createMutableCopy();
        batchedBlockRoots = state.getBatchedBlockRoots().createMutableCopy();

        latestEth1Data = state.getLatestEth1Data();
        eth1DataVotes = state.getEth1DataVotes().createMutableCopy();
  }

  @Override
  public BeaconState createImmutable() {
    return new BeaconStateImpl(this);
  }

  @Override
  public SlotNumber getSlot() {
    return slot;
  }

  @Override
  public void setSlot(SlotNumber slot) {
    this.slot = slot;
  }

  @Override
  public Time getGenesisTime() {
    return genesisTime;
  }

  @Override
  public void setGenesisTime(Time genesisTime) {
    this.genesisTime = genesisTime;
  }

  @Override
  public ForkData getForkData() {
    return forkData;
  }

  @Override
  public void setForkData(ForkData forkData) {
    this.forkData = forkData;
  }

  @Override
  public EpochNumber getValidatorRegistryUpdateEpoch() {
    return validatorRegistryUpdateEpoch;
  }

  @Override
  public void setValidatorRegistryUpdateEpoch(
      EpochNumber validatorRegistryUpdateEpoch) {
    this.validatorRegistryUpdateEpoch = validatorRegistryUpdateEpoch;
  }

  @Override
  public ShardNumber getPreviousShufflingStartShard() {
    return previousShufflingStartShard;
  }

  @Override
  public void setPreviousShufflingStartShard(
      ShardNumber previousShufflingStartShard) {
    this.previousShufflingStartShard = previousShufflingStartShard;
  }

  @Override
  public ShardNumber getCurrentShufflingStartShard() {
    return currentShufflingStartShard;
  }

  @Override
  public void setCurrentShufflingStartShard(ShardNumber currentShufflingStartShard) {
    this.currentShufflingStartShard = currentShufflingStartShard;
  }

  @Override
  public EpochNumber getPreviousShufflingEpoch() {
    return previousShufflingEpoch;
  }

  @Override
  public void setPreviousShufflingEpoch(
      EpochNumber previousShufflingEpoch) {
    this.previousShufflingEpoch = previousShufflingEpoch;
  }

  @Override
  public EpochNumber getCurrentShufflingEpoch() {
    return currentShufflingEpoch;
  }

  @Override
  public void setCurrentShufflingEpoch(
      EpochNumber currentShufflingEpoch) {
    this.currentShufflingEpoch = currentShufflingEpoch;
  }

  @Override
  public Hash32 getPreviousShufflingSeed() {
    return previousShufflingSeed;
  }

  @Override
  public void setPreviousShufflingSeed(Hash32 previousShufflingSeed) {
    this.previousShufflingSeed = previousShufflingSeed;
  }

  @Override
  public Hash32 getCurrentShufflingSeed() {
    return currentShufflingSeed;
  }

  @Override
  public void setCurrentShufflingSeed(Hash32 currentShufflingSeed) {
    this.currentShufflingSeed = currentShufflingSeed;
  }

  @Override
  public EpochNumber getPreviousJustifiedEpoch() {
    return previousJustifiedEpoch;
  }

  @Override
  public void setPreviousJustifiedEpoch(EpochNumber previousJustifiedEpoch) {
    this.previousJustifiedEpoch = previousJustifiedEpoch;
  }

  @Override
  public EpochNumber getJustifiedEpoch() {
    return justifiedEpoch;
  }

  public void setJustifiedEpoch(EpochNumber justifiedEpoch) {
    this.justifiedEpoch = justifiedEpoch;
  }

  @Override
  public Bitfield64 getJustificationBitfield() {
    return justificationBitfield;
  }

  @Override
  public void setJustificationBitfield(Bitfield64 justificationBitfield) {
    this.justificationBitfield = justificationBitfield;
  }

  @Override
  public EpochNumber getFinalizedEpoch() {
    return finalizedEpoch;
  }

  @Override
  public void setFinalizedEpoch(EpochNumber finalizedEpoch) {
    this.finalizedEpoch = finalizedEpoch;
  }

  @Override
  public Eth1Data getLatestEth1Data() {
    return latestEth1Data;
  }

  @Override
  public void setLatestEth1Data(Eth1Data latestEth1Data) {
    this.latestEth1Data = latestEth1Data;
  }

  @Override
  public UInt64 getDepositIndex() {
    return depositIndex;
  }

  @Override
  public void setDepositIndex(UInt64 depositIndex) {
    this.depositIndex = depositIndex;
  }

  @Override
  public WriteList<ValidatorIndex, ValidatorRecord> getValidatorRegistry() {
    return validatorRegistry;
  }

  public void setValidatorRegistry(
      WriteList<ValidatorIndex, ValidatorRecord> validatorRegistry) {
    this.validatorRegistry = validatorRegistry;
  }

  @Override
  public WriteList<ValidatorIndex, Gwei> getValidatorBalances() {
    return validatorBalances;
  }

  public void setValidatorBalances(
      WriteList<ValidatorIndex, Gwei> validatorBalances) {
    this.validatorBalances = validatorBalances;
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestRandaoMixes() {
    return latestRandaoMixes;
  }

  public void setLatestRandaoMixes(
      WriteList<EpochNumber, Hash32> latestRandaoMixes) {
    this.latestRandaoMixes = latestRandaoMixes;
  }

  @Override
  public WriteList<ShardNumber, Crosslink> getLatestCrosslinks() {
    return latestCrosslinks;
  }

  public void setLatestCrosslinks(
      WriteList<ShardNumber, Crosslink> latestCrosslinks) {
    this.latestCrosslinks = latestCrosslinks;
  }

  @Override
  public WriteList<SlotNumber, Hash32> getLatestBlockRoots() {
    return latestBlockRoots;
  }

  public void setLatestBlockRoots(
      WriteList<SlotNumber, Hash32> latestBlockRoots) {
    this.latestBlockRoots = latestBlockRoots;
  }

  @Override
  public WriteList<EpochNumber, Hash32> getLatestActiveIndexRoots() {
    return latestActiveIndexRoots;
  }

  public void setLatestActiveIndexRoots(
      WriteList<EpochNumber, Hash32> latestActiveIndexRoots) {
    this.latestActiveIndexRoots = latestActiveIndexRoots;
  }

  @Override
  public WriteList<EpochNumber, Gwei> getLatestSlashedBalances() {
    return latestSlashedBalances;
  }

  public void setLatestSlashedBalances(
      WriteList<EpochNumber, Gwei> latestSlashedBalances) {
    this.latestSlashedBalances = latestSlashedBalances;
  }

  @Override
  public WriteList<Integer, PendingAttestationRecord> getLatestAttestations() {
    return latestAttestations;
  }

  public void setLatestAttestations(
      WriteList<Integer, PendingAttestationRecord> latestAttestations) {
    this.latestAttestations = latestAttestations;
  }

  @Override
  public WriteList<Integer, Hash32> getBatchedBlockRoots() {
    return batchedBlockRoots;
  }

  public void setBatchedBlockRoots(
      WriteList<Integer, Hash32> batchedBlockRoots) {
    this.batchedBlockRoots = batchedBlockRoots;
  }

  @Override
  public WriteList<Integer, Eth1DataVote> getEth1DataVotes() {
    return eth1DataVotes;
  }

  public void setEth1DataVotes(
      WriteList<Integer, Eth1DataVote> eth1DataVotes) {
    this.eth1DataVotes = eth1DataVotes;
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

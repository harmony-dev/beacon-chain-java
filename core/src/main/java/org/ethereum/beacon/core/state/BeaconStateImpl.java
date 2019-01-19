package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  /* Misc */

  /** Slot number that this state was calculated in. */
  @SSZ private UInt64 slot;
  /** Timestamp of the genesis. */
  @SSZ private UInt64 genesisTime;
  /** Fork data corresponding to the {@link #slot}. */
  @SSZ private ForkData forkData;

  /* Validator registry */

  /** Validator registry records. */
  @SSZ private List<ValidatorRecord> validatorRegistry;
  /** Validator balances. */
  @SSZ private List<UInt64> validatorBalances;
  /** Slot number of last validator registry change. */
  @SSZ private UInt64 validatorRegistryLatestChangeSlot;
  /** A nonce for validator registry exits. */
  @SSZ private UInt64 validatorRegistryExitCount;
  /** A hash of latest validator registry delta. */
  @SSZ private Hash32 validatorRegistryDeltaChainTip;

  /* Randomness and committees */

  /** The most recent randao mixes. */
  @SSZ private List<Hash32> latestRandaoMixes;
  /** The most recent VDF outputs. */
  @SSZ private List<Hash32> latestVdfOutputs;

  @SSZ private UInt64 previousEpochStartShard;
  @SSZ private UInt64 currentEpochStartShard;
  @SSZ private UInt64 previousEpochCalculationSlot;
  @SSZ private UInt64 currentEpochCalculationSlot;
  @SSZ private Hash32 previousEpochRandaoMix;
  @SSZ private Hash32 currentEpochRandaoMix;

  /** Proof of custody placeholder. */
  @SSZ private List<CustodyChallenge> custodyChallenges;

  /* Finality */

  /** Latest justified slot before {@link #justifiedSlot}. */
  @SSZ private UInt64 previousJustifiedSlot;
  /** Latest justified slot. */
  @SSZ private UInt64 justifiedSlot;
  /** Bitfield of latest justified slots (epochs). */
  @SSZ private UInt64 justificationBitfield;
  /** Latest finalized slot. */
  @SSZ private UInt64 finalizedSlot;

  /* Recent state */

  /** Latest crosslink record for each shard. */
  @SSZ private List<CrosslinkRecord> latestCrosslinks;
  /** Latest block hashes for each shard. */
  @SSZ private List<Hash32> latestBlockRoots;
  /** Indices of validators that has been ejected lately. */
  @SSZ private List<UInt64> latestPenalizedExitBalances;
  /** Attestations that has not been processed yet. */
  @SSZ private List<PendingAttestationRecord> latestAttestations;
  /**
   * Latest hashes of {@link #latestBlockRoots} list calculated when its length got exceeded
   * LATEST_BLOCK_ROOTS_LENGTH.
   */
  @SSZ private List<Hash32> batchedBlockRoots;

  /* PoW receipt root */

  /** Latest processed receipt root from PoW deposit contract. */
  @SSZ private Hash32 latestDepositRoot;
  /** Receipt roots that voting is still in progress for. */
  @SSZ private List<DepositRootVote> depositRootVotes;

  public BeaconStateImpl() {}

  private BeaconStateImpl(BeaconState state) {
    this(
        state.getSlot(),
        state.getGenesisTime(),
        state.getForkData(),

        new ArrayList<>(state.getValidatorRegistry()),
        new ArrayList<>(state.getValidatorBalances()),
        state.getValidatorRegistryLatestChangeSlot(),
        state.getValidatorRegistryExitCount(),
        state.getValidatorRegistryDeltaChainTip(),

        new ArrayList<>(state.getLatestRandaoMixes()),
        new ArrayList<>(state.getLatestVdfOutputs()),

        state.getPreviousEpochStartShard(),
        state.getCurrentEpochStartShard(),
        state.getPreviousEpochCalculationSlot(),
        state.getCurrentEpochCalculationSlot(),
        state.getPreviousEpochRandaoMix(),
        state.getCurrentEpochRandaoMix(),

        new ArrayList<>(state.getCustodyChallenges()),

        state.getPreviousJustifiedSlot(),
        state.getJustifiedSlot(),
        state.getJustificationBitfield(),
        state.getFinalizedSlot(),

        new ArrayList<>(state.getLatestCrosslinks()),
        new ArrayList<>(state.getLatestBlockRoots()),
        new ArrayList<>(state.getLatestPenalizedExitBalances()),
        new ArrayList<>(state.getLatestAttestations()),
        new ArrayList<>(state.getBatchedBlockRoots()),

        state.getLatestDepositRoot(),
        new ArrayList<>(state.getDepositRootVotes()));
  }

  private BeaconStateImpl(
      UInt64 slot,
      UInt64 genesisTime,
      ForkData forkData,
      List<ValidatorRecord> validatorRegistry,
      List<UInt64> validatorBalances,
      UInt64 validatorRegistryLatestChangeSlot,
      UInt64 validatorRegistryExitCount,
      Hash32 validatorRegistryDeltaChainTip,
      List<Hash32> latestRandaoMixes,
      List<Hash32> latestVdfOutputs,
      UInt64 previousEpochStartShard,
      UInt64 currentEpochStartShard,
      UInt64 previousEpochCalculationSlot,
      UInt64 currentEpochCalculationSlot,
      Hash32 previousEpochRandaoMix,
      Hash32 currentEpochRandaoMix,
      List<CustodyChallenge> custodyChallenges,
      UInt64 previousJustifiedSlot,
      UInt64 justifiedSlot,
      UInt64 justificationBitfield,
      UInt64 finalizedSlot,
      List<CrosslinkRecord> latestCrosslinks,
      List<Hash32> latestBlockRoots,
      List<UInt64> latestPenalizedExitBalances,
      List<PendingAttestationRecord> latestAttestations,
      List<Hash32> batchedBlockRoots,
      Hash32 latestDepositRoot,
      List<DepositRootVote> depositRootVotes) {
    this.slot = slot;
    this.genesisTime = genesisTime;
    this.forkData = forkData;
    this.validatorRegistry = validatorRegistry;
    this.validatorBalances = validatorBalances;
    this.validatorRegistryLatestChangeSlot = validatorRegistryLatestChangeSlot;
    this.validatorRegistryExitCount = validatorRegistryExitCount;
    this.validatorRegistryDeltaChainTip = validatorRegistryDeltaChainTip;
    this.latestRandaoMixes = latestRandaoMixes;
    this.latestVdfOutputs = latestVdfOutputs;
    this.previousEpochStartShard = previousEpochStartShard;
    this.currentEpochStartShard = currentEpochStartShard;
    this.previousEpochCalculationSlot = previousEpochCalculationSlot;
    this.currentEpochCalculationSlot = currentEpochCalculationSlot;
    this.previousEpochRandaoMix = previousEpochRandaoMix;
    this.currentEpochRandaoMix = currentEpochRandaoMix;

    this.custodyChallenges = custodyChallenges;
    this.previousJustifiedSlot = previousJustifiedSlot;
    this.justifiedSlot = justifiedSlot;
    this.justificationBitfield = justificationBitfield;
    this.finalizedSlot = finalizedSlot;
    this.latestCrosslinks = latestCrosslinks;
    this.latestBlockRoots = latestBlockRoots;
    this.latestPenalizedExitBalances = latestPenalizedExitBalances;
    this.latestAttestations = latestAttestations;
    this.batchedBlockRoots = batchedBlockRoots;
    this.latestDepositRoot = latestDepositRoot;
    this.depositRootVotes = depositRootVotes;

    validate();
  }

  @Override
  public BeaconState validate() {
    // TODO
    return this;
  }

  @Override
  public UInt64 getSlot() {
    return slot;
  }

  @Override
  public void setSlot(UInt64 slot) {
    this.slot = slot;
  }

  @Override
  public UInt64 getGenesisTime() {
    return genesisTime;
  }

  @Override
  public void setGenesisTime(UInt64 genesisTime) {
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
  public List<ValidatorRecord> getValidatorRegistry() {
    return unmodifiableList(validatorRegistry);
  }

  @Override
  public void setValidatorRegistry(List<ValidatorRecord> validatorRegistry) {
    this.validatorRegistry = validatorRegistry;
  }

  @Override
  public List<UInt64> getValidatorBalances() {
    return unmodifiableList(validatorBalances);
  }

  @Override
  public void setValidatorBalances(List<UInt64> validatorBalances) {
    this.validatorBalances = validatorBalances;
  }

  @Override
  public UInt64 getValidatorRegistryLatestChangeSlot() {
    return validatorRegistryLatestChangeSlot;
  }

  @Override
  public void setValidatorRegistryLatestChangeSlot(UInt64 validatorRegistryLatestChangeSlot) {
    this.validatorRegistryLatestChangeSlot = validatorRegistryLatestChangeSlot;
  }

  @Override
  public UInt64 getValidatorRegistryExitCount() {
    return validatorRegistryExitCount;
  }

  @Override
  public void setValidatorRegistryExitCount(UInt64 validatorRegistryExitCount) {
    this.validatorRegistryExitCount = validatorRegistryExitCount;
  }

  @Override
  public Hash32 getValidatorRegistryDeltaChainTip() {
    return validatorRegistryDeltaChainTip;
  }

  @Override
  public void setValidatorRegistryDeltaChainTip(Hash32 validatorRegistryDeltaChainTip) {
    this.validatorRegistryDeltaChainTip = validatorRegistryDeltaChainTip;
  }

  @Override
  public List<Hash32> getLatestRandaoMixes() {
    return unmodifiableList(latestRandaoMixes);
  }

  @Override
  public void setLatestRandaoMixes(List<Hash32> latestRandaoMixes) {
    this.latestRandaoMixes = latestRandaoMixes;
  }

  @Override
  public List<Hash32> getLatestVdfOutputs() {
    return unmodifiableList(latestVdfOutputs);
  }

  @Override
  public void setLatestVdfOutputs(List<Hash32> latestVdfOutputs) {
    this.latestVdfOutputs = latestVdfOutputs;
  }

  @Override
  public UInt64 getPreviousEpochStartShard() {
    return previousEpochStartShard;
  }

  @Override
  public void setPreviousEpochStartShard(UInt64 previousEpochStartShard) {
    this.previousEpochStartShard = previousEpochStartShard;
  }

  @Override
  public UInt64 getCurrentEpochStartShard() {
    return currentEpochStartShard;
  }

  @Override
  public void setCurrentEpochStartShard(UInt64 currentEpochStartShard) {
    this.currentEpochStartShard = currentEpochStartShard;
  }

  @Override
  public UInt64 getPreviousEpochCalculationSlot() {
    return previousEpochCalculationSlot;
  }

  @Override
  public void setPreviousEpochCalculationSlot(UInt64 previousEpochCalculationSlot) {
    this.previousEpochCalculationSlot = previousEpochCalculationSlot;
  }

  @Override
  public UInt64 getCurrentEpochCalculationSlot() {
    return currentEpochCalculationSlot;
  }

  @Override
  public void setCurrentEpochCalculationSlot(UInt64 currentEpochCalculationSlot) {
    this.currentEpochCalculationSlot = currentEpochCalculationSlot;
  }

  @Override
  public Hash32 getPreviousEpochRandaoMix() {
    return previousEpochRandaoMix;
  }

  @Override
  public void setPreviousEpochRandaoMix(Hash32 previousEpochRandaoMix) {
    this.previousEpochRandaoMix = previousEpochRandaoMix;
  }

  @Override
  public Hash32 getCurrentEpochRandaoMix() {
    return currentEpochRandaoMix;
  }

  @Override
  public void setCurrentEpochRandaoMix(Hash32 currentEpochRandaoMix) {
    this.currentEpochRandaoMix = currentEpochRandaoMix;
  }

  @Override
  public List<CustodyChallenge> getCustodyChallenges() {
    return unmodifiableList(custodyChallenges);
  }

  @Override
  public void setCustodyChallenges(List<CustodyChallenge> custodyChallenges) {
    this.custodyChallenges = custodyChallenges;
  }

  @Override
  public UInt64 getPreviousJustifiedSlot() {
    return previousJustifiedSlot;
  }

  @Override
  public void setPreviousJustifiedSlot(UInt64 previousJustifiedSlot) {
    this.previousJustifiedSlot = previousJustifiedSlot;
  }

  @Override
  public UInt64 getJustifiedSlot() {
    return justifiedSlot;
  }

  @Override
  public void setJustifiedSlot(UInt64 justifiedSlot) {
    this.justifiedSlot = justifiedSlot;
  }

  @Override
  public UInt64 getJustificationBitfield() {
    return justificationBitfield;
  }

  @Override
  public void setJustificationBitfield(UInt64 justificationBitfield) {
    this.justificationBitfield = justificationBitfield;
  }

  @Override
  public UInt64 getFinalizedSlot() {
    return finalizedSlot;
  }

  @Override
  public void setFinalizedSlot(UInt64 finalizedSlot) {
    this.finalizedSlot = finalizedSlot;
  }

  @Override
  public List<CrosslinkRecord> getLatestCrosslinks() {
    return unmodifiableList(latestCrosslinks);
  }

  @Override
  public void setLatestCrosslinks(List<CrosslinkRecord> latestCrosslinks) {
    this.latestCrosslinks = latestCrosslinks;
  }

  @Override
  public List<Hash32> getLatestBlockRoots() {
    return unmodifiableList(latestBlockRoots);
  }

  @Override
  public void setLatestBlockRoots(List<Hash32> latestBlockRoots) {
    this.latestBlockRoots = latestBlockRoots;
  }

  @Override
  public List<UInt64> getLatestPenalizedExitBalances() {
    return unmodifiableList(latestPenalizedExitBalances);
  }

  @Override
  public void setLatestPenalizedExitBalances(List<UInt64> latestPenalizedExitBalances) {
    this.latestPenalizedExitBalances = latestPenalizedExitBalances;
  }

  @Override
  public List<PendingAttestationRecord> getLatestAttestations() {
    return unmodifiableList(latestAttestations);
  }

  @Override
  public void setLatestAttestations(List<PendingAttestationRecord> latestAttestations) {
    this.latestAttestations = latestAttestations;
  }

  @Override
  public List<Hash32> getBatchedBlockRoots() {
    return unmodifiableList(batchedBlockRoots);
  }

  @Override
  public void setBatchedBlockRoots(List<Hash32> batchedBlockRoots) {
    this.batchedBlockRoots = batchedBlockRoots;
  }

  @Override
  public Hash32 getLatestDepositRoot() {
    return latestDepositRoot;
  }

  @Override
  public void setLatestDepositRoot(Hash32 latestDepositRoot) {
    this.latestDepositRoot = latestDepositRoot;
  }

  @Override
  public List<DepositRootVote> getDepositRootVotes() {
    return unmodifiableList(depositRootVotes);
  }

  @Override
  public void setDepositRootVotes(List<DepositRootVote> depositRootVotes) {
    this.depositRootVotes = depositRootVotes;
  }

  @Override
  public Hash32 getHash() {
    return Hash32.ZERO;
  }

  @Override
  public MutableBeaconState createMutableCopy() {
    return new BeaconStateImpl(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BeaconStateImpl that = (BeaconStateImpl) o;
    return Objects.equal(slot, that.slot)
        && Objects.equal(genesisTime, that.genesisTime)
        && Objects.equal(forkData, that.forkData)
        && Objects.equal(validatorRegistry, that.validatorRegistry)
        && Objects.equal(validatorBalances, that.validatorBalances)
        && Objects.equal(validatorRegistryLatestChangeSlot, that.validatorRegistryLatestChangeSlot)
        && Objects.equal(validatorRegistryExitCount, that.validatorRegistryExitCount)
        && Objects.equal(validatorRegistryDeltaChainTip, that.validatorRegistryDeltaChainTip)
        && Objects.equal(latestRandaoMixes, that.latestRandaoMixes)
        && Objects.equal(latestVdfOutputs, that.latestVdfOutputs)
        && Objects.equal(previousEpochStartShard, that.previousEpochStartShard)
        && Objects.equal(currentEpochStartShard, that.currentEpochStartShard)
        && Objects.equal(previousEpochCalculationSlot, that.previousEpochCalculationSlot)
        && Objects.equal(currentEpochCalculationSlot, that.currentEpochCalculationSlot)
        && Objects.equal(previousEpochRandaoMix, that.previousEpochRandaoMix)
        && Objects.equal(currentEpochRandaoMix, that.currentEpochRandaoMix)
        && Objects.equal(custodyChallenges, that.custodyChallenges)
        && Objects.equal(previousJustifiedSlot, that.previousJustifiedSlot)
        && Objects.equal(justifiedSlot, that.justifiedSlot)
        && Objects.equal(justificationBitfield, that.justificationBitfield)
        && Objects.equal(finalizedSlot, that.finalizedSlot)
        && Objects.equal(latestCrosslinks, that.latestCrosslinks)
        && Objects.equal(latestBlockRoots, that.latestBlockRoots)
        && Objects.equal(latestPenalizedExitBalances, that.latestPenalizedExitBalances)
        && Objects.equal(latestAttestations, that.latestAttestations)
        && Objects.equal(batchedBlockRoots, that.batchedBlockRoots)
        && Objects.equal(latestDepositRoot, that.latestDepositRoot)
        && Objects.equal(depositRootVotes, that.depositRootVotes);
  }
}

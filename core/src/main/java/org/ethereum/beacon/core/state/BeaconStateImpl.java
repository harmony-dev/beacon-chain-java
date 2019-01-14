package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

@SSZSerializable
public class BeaconStateImpl implements MutableBeaconState {

  /* Misc */

  /** Slot number that this state was calculated in. */
  private UInt64 slot;
  /** Timestamp of the genesis. */
  private UInt64 genesisTime;
  /** Fork data corresponding to the {@link #slot}. */
  private ForkData forkData;

  /* Validator registry */

  /** Validator registry records. */
  private List<ValidatorRecord> validatorRegistry;
  /** Validator balances. */
  private List<UInt64> validatorBalances;
  /** Slot number of last validator registry change. */
  private UInt64 validatorRegistryLatestChangeSlot;
  /** A nonce for validator registry exits. */
  private UInt64 validatorRegistryExitCount;
  /** A hash of latest validator registry delta. */
  private Hash32 validatorRegistryDeltaChainTip;

  /* Randomness and committees */

  /** The most recent randao mixes. */
  private List<Hash32> latestRandaoMixes;
  /** The most recent VDF outputs. */
  private List<Hash32> latestVdfOutputs;
  /** Which committee assigned to which shard on which slot. */
  private List<List<ShardCommittee>> shardCommitteesAtSlots;

  /** Proof of custody placeholder. */
  private List<CustodyChallenge> custodyChallenges;

  /* Finality */

  /** Latest justified slot before {@link #justifiedSlot}. */
  private UInt64 previousJustifiedSlot;
  /** Latest justified slot. */
  private UInt64 justifiedSlot;
  /** Bitfield of latest justified slots (epochs). */
  private UInt64 justificationBitfield;
  /** Latest finalized slot. */
  private UInt64 finalizedSlot;

  /* Recent state */

  /** Latest crosslink record for each shard. */
  private List<CrosslinkRecord> latestCrosslinks;
  /** Latest block hashes for each shard. */
  private List<Hash32> latestBlockRoots;
  /** Indices of validators that has been ejected lately. */
  private List<UInt64> latestPenalizedExitBalances;
  /** Attestations that has not been processed yet. */
  private List<PendingAttestationRecord> latestAttestations;
  /**
   * Latest hashes of {@link #latestBlockRoots} list calculated when its length got exceeded
   * LATEST_BLOCK_ROOTS_LENGTH.
   */
  private List<Hash32> batchedBlockRoots;

  /* PoW receipt root */

  /** Latest processed receipt root from PoW deposit contract. */
  private Hash32 latestDepositRoot;
  /** Receipt roots that voting is still in progress for. */
  private List<DepositRootVote> depositRootVotes;

  public BeaconStateImpl() {
  }

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
        state.getShardCommitteesAtSlots().stream().map(ArrayList::new).collect(Collectors.toList()),

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
        new ArrayList<>(state.getDepositRootVotes())
    );
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
      List<List<ShardCommittee>> shardCommitteesAtSlots,
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
    this.shardCommitteesAtSlots = shardCommitteesAtSlots;
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
  public UInt64 getGenesisTime() {
    return genesisTime;
  }

  @Override
  public ForkData getForkData() {
    return forkData;
  }

  @Override
  public List<ValidatorRecord> getValidatorRegistry() {
    return unmodifiableList(validatorRegistry);
  }

  @Override
  public List<UInt64> getValidatorBalances() {
    return unmodifiableList(validatorBalances);
  }

  @Override
  public UInt64 getValidatorRegistryLatestChangeSlot() {
    return validatorRegistryLatestChangeSlot;
  }

  @Override
  public UInt64 getValidatorRegistryExitCount() {
    return validatorRegistryExitCount;
  }

  @Override
  public Hash32 getValidatorRegistryDeltaChainTip() {
    return validatorRegistryDeltaChainTip;
  }

  @Override
  public List<Hash32> getLatestRandaoMixes() {
    return unmodifiableList(latestRandaoMixes);
  }

  @Override
  public List<Hash32> getLatestVdfOutputs() {
    return unmodifiableList(latestVdfOutputs);
  }

  @Override
  public List<List<ShardCommittee>> getShardCommitteesAtSlots() {
    return unmodifiableList(shardCommitteesAtSlots.stream()
        .map(Collections::unmodifiableList).collect(Collectors.toList()));
  }

  @Override
  public List<CustodyChallenge> getCustodyChallenges() {
    return unmodifiableList(custodyChallenges);
  }

  @Override
  public UInt64 getPreviousJustifiedSlot() {
    return previousJustifiedSlot;
  }

  @Override
  public UInt64 getJustifiedSlot() {
    return justifiedSlot;
  }

  @Override
  public UInt64 getJustificationBitfield() {
    return justificationBitfield;
  }

  @Override
  public UInt64 getFinalizedSlot() {
    return finalizedSlot;
  }

  @Override
  public List<CrosslinkRecord> getLatestCrosslinks() {
    return unmodifiableList(latestCrosslinks);
  }

  @Override
  public List<Hash32> getLatestBlockRoots() {
    return unmodifiableList(latestBlockRoots);
  }

  @Override
  public List<UInt64> getLatestPenalizedExitBalances() {
    return unmodifiableList(latestPenalizedExitBalances);
  }

  @Override
  public List<PendingAttestationRecord> getLatestAttestations() {
    return unmodifiableList(latestAttestations);
  }

  @Override
  public List<Hash32> getBatchedBlockRoots() {
    return unmodifiableList(batchedBlockRoots);
  }

  @Override
  public Hash32 getLatestDepositRoot() {
    return latestDepositRoot;
  }

  @Override
  public List<DepositRootVote> getDepositRootVotes() {
    return unmodifiableList(depositRootVotes);
  }

  @Override
  public Hash32 getHash() {
    return Hash32.ZERO;
  }

  @Override
  public void setSlot(UInt64 slot) {
    this.slot = slot;
  }

  @Override
  public void setGenesisTime(UInt64 genesisTime) {
    this.genesisTime = genesisTime;
  }

  @Override
  public void setForkData(ForkData forkData) {
    this.forkData = forkData;
  }

  @Override
  public void setValidatorRegistry(List<ValidatorRecord> validatorRegistry) {
    this.validatorRegistry = validatorRegistry;
  }

  @Override
  public void setValidatorBalances(List<UInt64> validatorBalances) {
    this.validatorBalances = validatorBalances;
  }

  @Override
  public void setValidatorRegistryLatestChangeSlot(UInt64 validatorRegistryLatestChangeSlot) {
    this.validatorRegistryLatestChangeSlot = validatorRegistryLatestChangeSlot;
  }

  @Override
  public void setValidatorRegistryExitCount(UInt64 validatorRegistryExitCount) {
    this.validatorRegistryExitCount = validatorRegistryExitCount;
  }

  @Override
  public void setValidatorRegistryDeltaChainTip(Hash32 validatorRegistryDeltaChainTip) {
    this.validatorRegistryDeltaChainTip = validatorRegistryDeltaChainTip;
  }

  @Override
  public void setLatestRandaoMixes(List<Hash32> latestRandaoMixes) {
    this.latestRandaoMixes = latestRandaoMixes;
  }

  @Override
  public void setLatestVdfOutputs(List<Hash32> latestVdfOutputs) {
    this.latestVdfOutputs = latestVdfOutputs;
  }

  @Override
  public void setShardCommitteesAtSlots(ShardCommittee[][] shardCommitteesAtSlots) {
    List<List<ShardCommittee>> lists = Arrays.stream(shardCommitteesAtSlots)
        .map(cArr -> Arrays.stream(cArr).collect(Collectors.toList()))
        .collect(Collectors.toList());
  }

  @Override
  public void setShardCommitteesAtSlots(List<List<ShardCommittee>> shardCommitteesAtSlots) {
    this.shardCommitteesAtSlots = shardCommitteesAtSlots;
  }

  @Override
  public void setCustodyChallenges(List<CustodyChallenge> custodyChallenges) {
    this.custodyChallenges = custodyChallenges;
  }

  @Override
  public void setPreviousJustifiedSlot(UInt64 previousJustifiedSlot) {
    this.previousJustifiedSlot = previousJustifiedSlot;
  }

  @Override
  public void setJustifiedSlot(UInt64 justifiedSlot) {
    this.justifiedSlot = justifiedSlot;
  }

  @Override
  public void setJustificationBitfield(UInt64 justificationBitfield) {
    this.justificationBitfield = justificationBitfield;
  }

  @Override
  public void setFinalizedSlot(UInt64 finalizedSlot) {
    this.finalizedSlot = finalizedSlot;
  }

  @Override
  public void setLatestCrosslinks(List<CrosslinkRecord> latestCrosslinks) {
    this.latestCrosslinks = latestCrosslinks;
  }

  @Override
  public void setLatestBlockRoots(List<Hash32> latestBlockRoots) {
    this.latestBlockRoots = latestBlockRoots;
  }

  @Override
  public void setLatestPenalizedExitBalances(List<UInt64> latestPenalizedExitBalances) {
    this.latestPenalizedExitBalances = latestPenalizedExitBalances;
  }

  @Override
  public void setLatestAttestations(List<PendingAttestationRecord> latestAttestations) {
    this.latestAttestations = latestAttestations;
  }

  @Override
  public void setBatchedBlockRoots(List<Hash32> batchedBlockRoots) {
    this.batchedBlockRoots = batchedBlockRoots;
  }

  @Override
  public void setLatestDepositRoot(Hash32 latestDepositRoot) {
    this.latestDepositRoot = latestDepositRoot;
  }

  @Override
  public void setDepositRootVotes(List<DepositRootVote> depositRootVotes) {
    this.depositRootVotes = depositRootVotes;
  }

  @Override
  public MutableBeaconState createMutableCopy() {
    return new BeaconStateImpl(this);
  }
}

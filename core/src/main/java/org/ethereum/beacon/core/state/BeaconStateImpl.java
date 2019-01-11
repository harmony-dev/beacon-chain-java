package org.ethereum.beacon.core.state;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.CustodyChallenge;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconStateImpl implements BeaconState {

  public static final BeaconState EMPTY =
      new BeaconStateImpl(
          UInt64.ZERO,
          UInt64.ZERO,
          ForkData.EMPTY,
          emptyList(),
          emptyList(),
          UInt64.ZERO,
          UInt64.ZERO,
          Hash32.ZERO,
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          UInt64.ZERO,
          UInt64.ZERO,
          UInt64.ZERO,
          UInt64.ZERO,
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          Hash32.ZERO,
          emptyList());

  /* Misc */

  /** Slot number that this state was calculated in. */
  private final UInt64 slot;
  /** Timestamp of the genesis. */
  private final UInt64 genesisTime;
  /** Fork data corresponding to the {@link #slot}. */
  private final ForkData forkData;

  /* Validator registry */

  /** Validator registry records. */
  private final List<ValidatorRecord> validatorRegistry;
  /** Validator balances. */
  private final List<UInt64> validatorBalances;
  /** Slot number of last validator registry change. */
  private final UInt64 validatorRegistryLatestChangeSlot;
  /** A nonce for validator registry exits. */
  private final UInt64 validatorRegistryExitCount;
  /** A hash of latest validator registry delta. */
  private final Hash32 validatorRegistryDeltaChainTip;

  /* Randomness and committees */

  /** The most recent randao mixes. */
  private final List<Hash32> latestRandaoMixes;
  /** The most recent VDF outputs. */
  private final List<Hash32> latestVdfOutputs;
  /** Which committee assigned to which shard on which slot. */
  private final List<List<ShardCommittee>> shardCommitteesAtSlots;

  /** Proof of custody placeholder. */
  private final List<CustodyChallenge> custodyChallenges;

  /* Finality */

  /** Latest justified slot before {@link #justifiedSlot}. */
  private final UInt64 previousJustifiedSlot;
  /** Latest justified slot. */
  private final UInt64 justifiedSlot;
  /** Bitfield of latest justified slots (epochs). */
  private final UInt64 justificationBitfield;
  /** Latest finalized slot. */
  private final UInt64 finalizedSlot;

  /* Recent state */

  /** Latest crosslink record for each shard. */
  private final List<CrosslinkRecord> latestCrosslinks;
  /** Latest block hashes for each shard. */
  private final List<Hash32> latestBlockRoots;
  /** Indices of validators that has been ejected lately. */
  private final List<UInt64> latestPenalizedExitBalances;
  /** Attestations that has not been processed yet. */
  private final List<PendingAttestationRecord> latestAttestations;
  /**
   * Latest hashes of {@link #latestBlockRoots} list calculated when its length got exceeded
   * LATEST_BLOCK_ROOTS_LENGTH.
   */
  private final List<Hash32> batchedBlockRoots;

  /* PoW receipt root */

  /** Latest processed receipt root from PoW deposit contract. */
  private final Hash32 latestDepositRoot;
  /** Receipt roots that voting is still in progress for. */
  private final List<DepositRootVote> depositRootVotes;

  public BeaconStateImpl(
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

  public static class Builder {

    /* Misc */
    private UInt64 slot;
    private UInt64 genesisTime;
    private ForkData forkData;

    /* Validator registry */
    private List<ValidatorRecord> validatorRegistry;
    private List<UInt64> validatorBalances;
    private UInt64 validatorRegistryLatestChangeSlot;
    private UInt64 validatorRegistryExitCount;
    private Hash32 validatorRegistryDeltaChainTip;

    /* Randomness and committees */
    private List<Hash32> latestRandaoMixes;
    private List<Hash32> latestVdfOutputs;
    private List<List<ShardCommittee>> shardCommitteesAtSlots;

    /* Proof of custody placeholder. */
    private List<CustodyChallenge> custodyChallenges;

    /* Finality */
    private UInt64 previousJustifiedSlot;
    private UInt64 justifiedSlot;
    private UInt64 justificationBitfield;
    private UInt64 finalizedSlot;

    /* Recent state */

    private List<CrosslinkRecord> latestCrosslinks;
    private List<Hash32> latestBlockRoots;
    private List<UInt64> latestPenalizedExitBalances;
    private List<PendingAttestationRecord> latestAttestations;
    private List<Hash32> batchedBlockRoots;

    /* PoW receipt root */
    private Hash32 latestDepositRoot;
    private List<DepositRootVote> depositRootVotes;

    private Builder() {}

    public static Builder fromState(BeaconState state) {
      Builder builder = new Builder();

      builder.slot = state.getSlot();
      builder.genesisTime = state.getGenesisTime();
      builder.forkData = state.getForkData();

      builder.validatorRegistry = state.getValidatorRegistry();
      builder.validatorBalances = state.getValidatorBalances();
      builder.validatorRegistryLatestChangeSlot = state.getValidatorRegistryLatestChangeSlot();
      builder.validatorRegistryExitCount = state.getValidatorRegistryExitCount();
      builder.validatorRegistryDeltaChainTip = state.getValidatorRegistryDeltaChainTip();

      builder.latestRandaoMixes = state.getLatestRandaoMixes();
      builder.latestVdfOutputs = state.getLatestVdfOutputs();
      builder.shardCommitteesAtSlots = state.getShardCommitteesAtSlots();

      builder.custodyChallenges = state.getCustodyChallenges();

      builder.previousJustifiedSlot = state.getPreviousJustifiedSlot();
      builder.justifiedSlot = state.getJustifiedSlot();
      builder.justificationBitfield = state.getJustificationBitfield();
      builder.finalizedSlot = state.getFinalizedSlot();

      builder.latestCrosslinks = state.getLatestCrosslinks();
      builder.latestBlockRoots = state.getLatestBlockRoots();
      builder.latestPenalizedExitBalances = state.getLatestPenalizedExitBalances();
      builder.latestAttestations = state.getLatestAttestations();
      builder.batchedBlockRoots = state.getBatchedBlockRoots();

      builder.latestDepositRoot = state.getLatestDepositRoot();
      builder.depositRootVotes = state.getDepositRootVotes();

      return builder;
    }

    public static Builder createEmpty() {
      return new Builder();
    }

    /**
     * A shortcut to {@link #fromState(BeaconState)} with {@code state} parameter set to {@link
     * #EMPTY} value.
     *
     * <p><strong>Note:</strong> these defaults are not the defaults from initial state.
     *
     * @return a builder.
     */
    public static Builder createWithDefaults() {
      return fromState(EMPTY);
    }

    public BeaconState build() {
      assert slot != null;
      assert genesisTime != null;
      assert forkData != null;
      assert validatorRegistry != null;
      assert validatorBalances != null;
      assert validatorRegistryLatestChangeSlot != null;
      assert validatorRegistryExitCount != null;
      assert validatorRegistryDeltaChainTip != null;
      assert latestRandaoMixes != null;
      assert latestVdfOutputs != null;
      assert shardCommitteesAtSlots != null;
      assert custodyChallenges != null;
      assert previousJustifiedSlot != null;
      assert justifiedSlot != null;
      assert justificationBitfield != null;
      assert finalizedSlot != null;
      assert latestCrosslinks != null;
      assert latestBlockRoots != null;
      assert latestPenalizedExitBalances != null;
      assert latestAttestations != null;
      assert batchedBlockRoots != null;
      assert latestDepositRoot != null;
      assert depositRootVotes != null;

      return new BeaconStateImpl(
          slot,
          genesisTime,
          forkData,
          validatorRegistry,
          validatorBalances,
          validatorRegistryLatestChangeSlot,
          validatorRegistryExitCount,
          validatorRegistryDeltaChainTip,
          latestRandaoMixes,
          latestVdfOutputs,
          shardCommitteesAtSlots,
          custodyChallenges,
          previousJustifiedSlot,
          justifiedSlot,
          justificationBitfield,
          finalizedSlot,
          latestCrosslinks,
          latestBlockRoots,
          latestPenalizedExitBalances,
          latestAttestations,
          batchedBlockRoots,
          latestDepositRoot,
          depositRootVotes);
    }

    public Builder withSlot(UInt64 slot) {
      this.slot = slot;
      return this;
    }

    public Builder withGenesisTime(UInt64 genesisTime) {
      this.genesisTime = genesisTime;
      return this;
    }

    public Builder withForkData(ForkData forkData) {
      this.forkData = forkData;
      return this;
    }

    public Builder withValidatorRegistry(List<ValidatorRecord> validatorRegistry) {
      this.validatorRegistry = validatorRegistry;
      return this;
    }

    public Builder withValidatorBalances(List<UInt64> validatorBalances) {
      this.validatorBalances = validatorBalances;
      return this;
    }

    public Builder withValidatorRegistryLatestChangeSlot(UInt64 validatorRegistryLatestChangeSlot) {
      this.validatorRegistryLatestChangeSlot = validatorRegistryLatestChangeSlot;
      return this;
    }

    public Builder withValidatorRegistryExitCount(UInt64 validatorRegistryExitCount) {
      this.validatorRegistryExitCount = validatorRegistryExitCount;
      return this;
    }

    public Builder withValidatorRegistryDeltaChainTip(Hash32 validatorRegistryDeltaChainTip) {
      this.validatorRegistryDeltaChainTip = validatorRegistryDeltaChainTip;
      return this;
    }

    public Builder withLatestRandaoMixes(List<Hash32> latestRandaoMixes) {
      this.latestRandaoMixes = latestRandaoMixes;
      return this;
    }

    public Builder withLatestVdfOutputs(List<Hash32> latestVdfOutputs) {
      this.latestVdfOutputs = latestVdfOutputs;
      return this;
    }

    public Builder withShardCommitteesAtSlots(ShardCommittee[][] shardCommitteesAtSlots) {
      List<List<ShardCommittee>> lists = Arrays.stream(shardCommitteesAtSlots)
          .map(cArr -> Arrays.stream(cArr).collect(Collectors.toList()))
          .collect(Collectors.toList());
      return withShardCommitteesAtSlots(lists);
    }

    public Builder withShardCommitteesAtSlots(List<List<ShardCommittee>> shardCommitteesAtSlots) {
      this.shardCommitteesAtSlots = shardCommitteesAtSlots;
      return this;
    }

    public Builder withCustodyChallenges(List<CustodyChallenge> custodyChallenges) {
      this.custodyChallenges = custodyChallenges;
      return this;
    }

    public Builder withPreviousJustifiedSlot(UInt64 previousJustifiedSlot) {
      this.previousJustifiedSlot = previousJustifiedSlot;
      return this;
    }

    public Builder withJustifiedSlot(UInt64 justifiedSlot) {
      this.justifiedSlot = justifiedSlot;
      return this;
    }

    public Builder withJustificationBitfield(UInt64 justificationBitfield) {
      this.justificationBitfield = justificationBitfield;
      return this;
    }

    public Builder withFinalizedSlot(UInt64 finalizedSlot) {
      this.finalizedSlot = finalizedSlot;
      return this;
    }

    public Builder withLatestCrosslinks(List<CrosslinkRecord> latestCrosslinks) {
      this.latestCrosslinks = latestCrosslinks;
      return this;
    }

    public Builder withLatestBlockRoots(List<Hash32> latestBlockRoots) {
      this.latestBlockRoots = latestBlockRoots;
      return this;
    }

    public Builder withLatestPenalizedExitBalances(List<UInt64> latestPenalizedExitBalances) {
      this.latestPenalizedExitBalances = latestPenalizedExitBalances;
      return this;
    }

    public Builder withLatestAttestations(List<PendingAttestationRecord> latestAttestations) {
      this.latestAttestations = latestAttestations;
      return this;
    }

    public Builder withBatchedBlockRoots(List<Hash32> batchedBlockRoots) {
      this.batchedBlockRoots = batchedBlockRoots;
      return this;
    }

    public Builder withLatestDepositRoot(Hash32 latestDepositRoot) {
      this.latestDepositRoot = latestDepositRoot;
      return this;
    }

    public Builder withDepositRootVotes(List<DepositRootVote> depositRootVotes) {
      this.depositRootVotes = depositRootVotes;
      return this;
    }
  }
}

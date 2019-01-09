package org.ethereum.beacon.core;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.state.CandidatePowReceiptRootRecord;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ShardCommittees;
import org.ethereum.beacon.core.state.ValidatorRecord;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Beacon chain state.
 *
 * @see BeaconBlock
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#beaconstate">BeaconState
 *     in the spec</a>
 */
public class BeaconState implements Hashable {

  /** Max number of RANDAO mixes kept by {@link #latestRandaoMixes}. */
  public static final int LATEST_RANDAO_MIXES_LENGTH = 1 << 13; // 8192
  /**
   * Max length of {@link #latestBlockRoots} list. After it gets exceeded hash of this list is added
   * to {@link #batchedBlockRoots}.
   */
  public static final int LATEST_BLOCK_ROOTS_LENGTH = 1 << 13; // 8192

  public static final BeaconState EMPTY =
      new BeaconState(
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
          ShardCommittees.EMPTY,
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
  private final ShardCommittee[][] shardCommitteesAtSlots;

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
   * Latest hashes of {@link #latestBlockRoots} list calculated when its length got exceeded {@link
   * #LATEST_BLOCK_ROOTS_LENGTH}.
   */
  private final List<Hash32> batchedBlockRoots;

  /* PoW receipt root */

  /** Latest processed receipt root from PoW deposit contract. */
  private final Hash32 processedPowReceiptRoot;
  /** Receipt roots that voting is still in progress for. */
  private final List<CandidatePowReceiptRootRecord> candidatePowReceiptRoots;

  public BeaconState(
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
      ShardCommittee[][] shardCommitteesAtSlots,
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
      Hash32 processedPowReceiptRoot,
      List<CandidatePowReceiptRootRecord> candidatePowReceiptRoots) {
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
    this.processedPowReceiptRoot = processedPowReceiptRoot;
    this.candidatePowReceiptRoots = candidatePowReceiptRoots;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public UInt64 getGenesisTime() {
    return genesisTime;
  }

  public ForkData getForkData() {
    return forkData;
  }

  public List<ValidatorRecord> extractValidatorRegistry() {
    return new ArrayList<>(validatorRegistry);
  }

  public List<ValidatorRecord> getValidatorRegistryUnsafe() {
    return validatorRegistry;
  }

  public List<UInt64> extractValidatorBalances() {
    return new ArrayList<>(validatorBalances);
  }

  public List<UInt64> getValidatorBalancesUnsafe() {
    return validatorBalances;
  }

  public UInt64 getValidatorRegistryLatestChangeSlot() {
    return validatorRegistryLatestChangeSlot;
  }

  public UInt64 getValidatorRegistryExitCount() {
    return validatorRegistryExitCount;
  }

  public Hash32 getValidatorRegistryDeltaChainTip() {
    return validatorRegistryDeltaChainTip;
  }

  public List<Hash32> getLatestRandaoMixesUnsafe() {
    return latestRandaoMixes;
  }

  public List<Hash32> extractLatestRandaoMixes() {
    return new ArrayList<>(latestRandaoMixes);
  }

  public List<Hash32> getLatestVdfOutputsUnsafe() {
    return latestVdfOutputs;
  }

  public List<Hash32> extractLatestVdfOutputs() {
    return new ArrayList<>(latestVdfOutputs);
  }

  public ShardCommittee[][] getShardCommitteesAtSlotsUnsafe() {
    return shardCommitteesAtSlots;
  }

  public List<CustodyChallenge> getCustodyChallengesUnsafe() {
    return custodyChallenges;
  }

  public List<CustodyChallenge> extractCustodyChallenges() {
    return new ArrayList<>(custodyChallenges);
  }

  public UInt64 getPreviousJustifiedSlot() {
    return previousJustifiedSlot;
  }

  public UInt64 getJustifiedSlot() {
    return justifiedSlot;
  }

  public UInt64 getJustificationBitfield() {
    return justificationBitfield;
  }

  public UInt64 getFinalizedSlot() {
    return finalizedSlot;
  }

  public List<CrosslinkRecord> getLatestCrosslinksUnsafe() {
    return latestCrosslinks;
  }

  public List<CrosslinkRecord> extractLatestCrosslinks() {
    return new ArrayList<>(latestCrosslinks);
  }

  public List<Hash32> getLatestBlockRootsUnsafe() {
    return latestBlockRoots;
  }

  public List<Hash32> extractLatestBlockRoots() {
    return new ArrayList<>(latestBlockRoots);
  }

  public List<UInt64> getLatestPenalizedExitBalancesUnsafe() {
    return latestPenalizedExitBalances;
  }

  public List<UInt64> extractLatestPenalizedExitBalances() {
    return new ArrayList<>(latestPenalizedExitBalances);
  }

  public List<PendingAttestationRecord> getLatestAttestationsUnsafe() {
    return latestAttestations;
  }

  public List<PendingAttestationRecord> extractLatestAttestations() {
    return new ArrayList<>(latestAttestations);
  }

  public List<Hash32> getBatchedBlockRootsUnsafe() {
    return batchedBlockRoots;
  }

  public List<Hash32> extractBatchedBlockRoots() {
    return new ArrayList<>(batchedBlockRoots);
  }

  public Hash32 getProcessedPowReceiptRoot() {
    return processedPowReceiptRoot;
  }

  public List<CandidatePowReceiptRootRecord> getCandidatePowReceiptRootsUnsafe() {
    return candidatePowReceiptRoots;
  }

  public List<CandidatePowReceiptRootRecord> extractCandidatePowReceiptRoots() {
    return new ArrayList<>(candidatePowReceiptRoots);
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
    private ShardCommittee[][] shardCommitteesAtSlots;

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
    private Hash32 processedPowReceiptRoot;
    private List<CandidatePowReceiptRootRecord> candidatePowReceiptRoots;

    private Builder() {}

    public static Builder fromState(BeaconState state) {
      Builder builder = new Builder();

      builder.slot = state.slot;
      builder.genesisTime = state.genesisTime;
      builder.forkData = state.forkData;

      builder.validatorRegistry = state.validatorRegistry;
      builder.validatorBalances = state.validatorBalances;
      builder.validatorRegistryLatestChangeSlot = state.validatorRegistryLatestChangeSlot;
      builder.validatorRegistryExitCount = state.validatorRegistryExitCount;
      builder.validatorRegistryDeltaChainTip = state.validatorRegistryDeltaChainTip;

      builder.latestRandaoMixes = state.latestRandaoMixes;
      builder.latestVdfOutputs = state.latestVdfOutputs;
      builder.shardCommitteesAtSlots = state.shardCommitteesAtSlots;

      builder.custodyChallenges = state.custodyChallenges;

      builder.previousJustifiedSlot = state.previousJustifiedSlot;
      builder.justifiedSlot = state.justifiedSlot;
      builder.justificationBitfield = state.justificationBitfield;
      builder.finalizedSlot = state.finalizedSlot;

      builder.latestCrosslinks = state.latestCrosslinks;
      builder.latestBlockRoots = state.latestBlockRoots;
      builder.latestPenalizedExitBalances = state.latestPenalizedExitBalances;
      builder.latestAttestations = state.latestAttestations;
      builder.batchedBlockRoots = state.batchedBlockRoots;

      builder.processedPowReceiptRoot = state.processedPowReceiptRoot;
      builder.candidatePowReceiptRoots = state.candidatePowReceiptRoots;

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
      assert processedPowReceiptRoot != null;
      assert candidatePowReceiptRoots != null;

      return new BeaconState(
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
          processedPowReceiptRoot,
          candidatePowReceiptRoots);
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

    public Builder withProcessedPowReceiptRoot(Hash32 processedPowReceiptRoot) {
      this.processedPowReceiptRoot = processedPowReceiptRoot;
      return this;
    }

    public Builder withCandidatePowReceiptRoots(
        List<CandidatePowReceiptRootRecord> candidatePowReceiptRoots) {
      this.candidatePowReceiptRoots = candidatePowReceiptRoots;
      return this;
    }
  }
}

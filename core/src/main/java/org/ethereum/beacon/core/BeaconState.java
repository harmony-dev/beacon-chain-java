package org.ethereum.beacon.core;

import static java.util.Collections.emptyList;

import java.util.List;
import org.ethereum.beacon.core.operations.ProofOfCustodyChallenge;
import org.ethereum.beacon.core.state.CandidatePowReceiptRootRecord;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.state.ShardReassignmentRecord;
import org.ethereum.beacon.core.state.ValidatorRecord;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt24;
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
          new ShardCommittee[0][],
          new UInt24[0][],
          emptyList(),
          emptyList(),
          UInt64.ZERO,
          UInt64.ZERO,
          UInt64.ZERO,
          UInt64.ZERO,
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
  /** Validator indices assigned to each shard to produce shard chains. */
  private final UInt24[][] persistentCommittees;
  /** Pending changes to {@link #persistentCommittees}. */
  private final List<ShardReassignmentRecord> persistentCommitteeReassignments;

  /** Proof of custody placeholder. */
  private final List<ProofOfCustodyChallenge> pocChallenges;

  /* Finality */

  /** Latest justified slot before {@link #justifiedSlot}. */
  private final UInt64 previousJustifiedSlot;
  /** Latest justified slot. */
  private final UInt64 justifiedSlot;
  /** Bitfield of latest justified slots (epochs). */
  private final UInt64 justificationBitfield;
  /** Latest finalized slot. */
  private final UInt64 finalizedSlot;

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
      UInt24[][] persistentCommittees,
      List<ShardReassignmentRecord> persistentCommitteeReassignments,
      List<ProofOfCustodyChallenge> pocChallenges,
      UInt64 previousJustifiedSlot,
      UInt64 justifiedSlot,
      UInt64 justificationBitfield,
      UInt64 finalizedSlot,
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
    this.persistentCommittees = persistentCommittees;
    this.persistentCommitteeReassignments = persistentCommitteeReassignments;
    this.pocChallenges = pocChallenges;
    this.previousJustifiedSlot = previousJustifiedSlot;
    this.justifiedSlot = justifiedSlot;
    this.justificationBitfield = justificationBitfield;
    this.finalizedSlot = finalizedSlot;
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

  public List<ValidatorRecord> getValidatorRegistry() {
    return validatorRegistry;
  }

  public List<UInt64> getValidatorBalances() {
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

  public List<Hash32> getLatestRandaoMixes() {
    return latestRandaoMixes;
  }

  public List<Hash32> getLatestVdfOutputs() {
    return latestVdfOutputs;
  }

  public ShardCommittee[][] getShardCommitteesAtSlots() {
    return shardCommitteesAtSlots;
  }

  public UInt24[][] getPersistentCommittees() {
    return persistentCommittees;
  }

  public List<ShardReassignmentRecord> getPersistentCommitteeReassignments() {
    return persistentCommitteeReassignments;
  }

  public List<ProofOfCustodyChallenge> getPocChallenges() {
    return pocChallenges;
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

  public Hash32 getProcessedPowReceiptRoot() {
    return processedPowReceiptRoot;
  }

  public List<CandidatePowReceiptRootRecord> getCandidatePowReceiptRoots() {
    return candidatePowReceiptRoots;
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
    private UInt24[][] persistentCommittees;
    private List<ShardReassignmentRecord> persistentCommitteeReassignments;

    /* Proof of custody placeholder. */
    private List<ProofOfCustodyChallenge> pocChallenges;

    /* Finality */
    private UInt64 previousJustifiedSlot;
    private UInt64 justifiedSlot;
    private UInt64 justificationBitfield;
    private UInt64 finalizedSlot;

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
      builder.persistentCommittees = state.persistentCommittees;
      builder.persistentCommitteeReassignments = state.persistentCommitteeReassignments;

      builder.pocChallenges = state.pocChallenges;

      builder.previousJustifiedSlot = state.previousJustifiedSlot;
      builder.justifiedSlot = state.justifiedSlot;
      builder.justificationBitfield = state.justificationBitfield;
      builder.finalizedSlot = state.finalizedSlot;

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
      assert persistentCommittees != null;
      assert persistentCommitteeReassignments != null;
      assert pocChallenges != null;
      assert previousJustifiedSlot != null;
      assert justifiedSlot != null;
      assert justificationBitfield != null;
      assert finalizedSlot != null;
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
          persistentCommittees,
          persistentCommitteeReassignments,
          pocChallenges,
          previousJustifiedSlot,
          justifiedSlot,
          justificationBitfield,
          finalizedSlot,
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

    public Builder withPersistentCommittees(UInt24[][] persistentCommittees) {
      this.persistentCommittees = persistentCommittees;
      return this;
    }

    public Builder withPersistentCommitteeReassignments(
        List<ShardReassignmentRecord> persistentCommitteeReassignments) {
      this.persistentCommitteeReassignments = persistentCommitteeReassignments;
      return this;
    }

    public Builder withPocChallenges(List<ProofOfCustodyChallenge> pocChallenges) {
      this.pocChallenges = pocChallenges;
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

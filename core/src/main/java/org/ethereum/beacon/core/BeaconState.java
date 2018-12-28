package org.ethereum.beacon.core;

import static java.util.Collections.emptyList;

import java.util.List;
import org.ethereum.beacon.core.operations.ProofOfCustodyChallenge;
import org.ethereum.beacon.core.state.CandidatePowReceiptRootRecord;
import org.ethereum.beacon.core.state.ForkData;
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
}

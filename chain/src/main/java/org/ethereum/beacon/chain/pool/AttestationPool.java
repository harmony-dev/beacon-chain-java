package org.ethereum.beacon.chain.pool;

import java.time.Duration;
import org.ethereum.beacon.chain.BeaconChain;
import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.chain.pool.checker.SignatureEncodingChecker;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.chain.pool.churn.OffChainAggregates;
import org.ethereum.beacon.chain.pool.registry.ProcessedAttestations;
import org.ethereum.beacon.chain.pool.registry.UnknownAttestationPool;
import org.ethereum.beacon.chain.pool.verifier.AttestationVerifier;
import org.ethereum.beacon.chain.pool.verifier.BatchVerifier;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.schedulers.Schedulers;
import org.reactivestreams.Publisher;

/**
 * Attestation pool API.
 *
 * <p>Along with {@link BeaconChain} attestation pool is one of the central components of the Beacon
 * chain client. Its main responsibilities are to verify attestation coming from the wire and
 * accumulate those of them which are not yet included on chain.
 *
 * <p>A list of attestation pool clients:
 *
 * <ul>
 *   <li>Wire - valid attestations should be further propagated; peers sent attestations that are
 *       eventually invalid must be dropped.
 *   <li>Fork choice - LMD GHOST is driven by attestations received from the wire even if they are
 *       not yet on chain.
 *   <li>Validator - attestations not yet included on chain should be included into a new block
 *       produced by proposer.
 * </ul>
 */
public interface AttestationPool {

  /** A number of threads in the executor processing attestation pool. */
  int MAX_THREADS = 32;

  /** Discard attestations with target epoch greater than current epoch plus this number. */
  EpochNumber MAX_ATTESTATION_LOOKAHEAD = EpochNumber.of(1);

  /**
   * Max number of attestations kept by processed attestations registry. An entry of this registry
   * should be represented by a hash of registered attestation.
   */
  int MAX_PROCESSED_ATTESTATIONS = 1_000_000;

  /** Max number of attestations made to not yet known block that could be kept in memory. */
  int MAX_UNKNOWN_ATTESTATIONS = 100_000;

  /** Max size of a buffer that collects attestations before passing them on the main verifier. */
  int VERIFIER_BUFFER_SIZE = 10_000;

  /** A throttling interval for verifier buffer. */
  Duration VERIFIER_INTERVAL = Duration.ofMillis(50);

  /**
   * Valid attestations publisher.
   *
   * @return a publisher.
   */
  Publisher<ReceivedAttestation> getValid();

  /**
   * Invalid attestations publisher.
   *
   * @return a publisher.
   */
  Publisher<ReceivedAttestation> getInvalid();

  /**
   * Publishes attestations which block is not yet a known block.
   *
   * <p>These attestations should be passed to a wire module in order to request a block.
   *
   * @return a publisher.
   */
  Publisher<ReceivedAttestation> getUnknownAttestations();

  /**
   * Publishes aggregated attestations that are not yet included on chain.
   *
   * <p>It should be a source of attestations for block proposer.
   *
   * @return a publisher.
   */
  Publisher<OffChainAggregates> getAggregates();

  /** Launches the pool. */
  void start();

  static AttestationPool create(
      Publisher<ReceivedAttestation> source,
      Publisher<SlotNumber> newSlots,
      Publisher<Checkpoint> finalizedCheckpoints,
      Publisher<BeaconBlock> importedBlocks,
      Publisher<BeaconBlock> chainHeads,
      Schedulers schedulers,
      BeaconChainSpec spec,
      BeaconChainStorage storage,
      EmptySlotTransition emptySlotTransition) {

    TimeFrameFilter timeFrameFilter = new TimeFrameFilter(spec, MAX_ATTESTATION_LOOKAHEAD);
    SanityChecker sanityChecker = new SanityChecker(spec);
    SignatureEncodingChecker encodingChecker = new SignatureEncodingChecker();
    ProcessedAttestations processedFilter =
        new ProcessedAttestations(spec::hash_tree_root, MAX_PROCESSED_ATTESTATIONS);
    UnknownAttestationPool unknownAttestationPool =
        new UnknownAttestationPool(
            storage.getBlockStorage(), spec, MAX_ATTESTATION_LOOKAHEAD, MAX_UNKNOWN_ATTESTATIONS);
    BatchVerifier batchVerifier =
        new AttestationVerifier(storage.getTupleStorage(), spec, emptySlotTransition);

    return new InMemoryAttestationPool(
        source,
        newSlots,
        finalizedCheckpoints,
        importedBlocks,
        chainHeads,
        schedulers,
        timeFrameFilter,
        sanityChecker,
        encodingChecker,
        processedFilter,
        unknownAttestationPool,
        batchVerifier);
  }
}

package org.ethereum.beacon.chain.pool;

import java.time.Duration;
import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.chain.pool.checker.SignatureEncodingChecker;
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

public interface AttestationPool {

  int MAX_THREADS = 32;

  EpochNumber MAX_ATTESTATION_LOOKAHEAD = EpochNumber.of(1);

  int MAX_KNOWN_ATTESTATIONS = 1_000_000;

  int UNKNOWN_ATTESTATION_POOL_SIZE = 100_000;

  int VERIFIER_BUFFER_SIZE = 10_000;

  Duration VERIFIER_INTERVAL = Duration.ofMillis(50);

  Publisher<ReceivedAttestation> getValid();

  Publisher<ReceivedAttestation> getInvalid();

  Publisher<ReceivedAttestation> getUnknownAttestations();

  Publisher<OffChainAggregates> getAggregates();

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

    SanityChecker sanityChecker = new SanityChecker(spec);
    SignatureEncodingChecker encodingChecker = new SignatureEncodingChecker();
    ProcessedAttestations processedFilter =
        new ProcessedAttestations(spec::hash_tree_root, MAX_KNOWN_ATTESTATIONS);
    UnknownAttestationPool unknownAttestationPool =
        new UnknownAttestationPool(
            storage.getBlockStorage(),
            spec,
            MAX_ATTESTATION_LOOKAHEAD,
            UNKNOWN_ATTESTATION_POOL_SIZE);
    BatchVerifier batchVerifier =
        new AttestationVerifier(storage.getTupleStorage(), spec, emptySlotTransition);

    return new InMemoryAttestationPool(
        source,
        newSlots,
        finalizedCheckpoints,
        importedBlocks,
        chainHeads,
        schedulers,
        sanityChecker,
        encodingChecker,
        processedFilter,
        unknownAttestationPool,
        batchVerifier);
  }
}

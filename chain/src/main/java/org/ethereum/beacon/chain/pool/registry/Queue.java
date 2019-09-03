package org.ethereum.beacon.chain.pool.registry;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.core.types.EpochNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maintains attestations parked by {@link UnknownAttestationPool}.
 *
 * <p>In terms of a number of parked attestations this queue holds LRU contract. Given a new
 * attestation if overall attestation number exceeds {@link #maxSize} the earliest added attestation
 * will be purged from the queue.
 *
 * <p><strong>Note:</strong> this implementation is not thread-safe.
 */
final class Queue {

  /** A queue is a list of epoch buckets. See {@link EpochBucket}. */
  private final LinkedList<EpochBucket> queue = new LinkedList<>();
  /** A number of epochs held by the {@link #queue}. */
  private final EpochNumber trackedEpochs;
  /** Max number of overall parked attestations. */
  private final long maxSize;
  /** A lower time frame boundary of attestation queue. */
  private EpochNumber baseLine;

  Queue(EpochNumber trackedEpochs, long maxSize) {
    assert maxSize > 0;
    assert trackedEpochs.getValue() > 0;

    this.trackedEpochs = trackedEpochs;
    this.maxSize = maxSize;
    this.baseLine = EpochNumber.ZERO;
  }

  /**
   * Moves base line forward.
   *
   * <p>Removes attestations made to epochs standing behind new base line.
   *
   * @param newBaseLine a new base line.
   */
  void moveBaseLine(EpochNumber newBaseLine) {
    assert baseLine == null || newBaseLine.greater(baseLine);

    for (long i = 0; i < newBaseLine.minus(baseLine).getValue() && queue.size() > 0; i++) {
      queue.removeFirst();
    }

    while (queue.size() < trackedEpochs.getValue()) {
      queue.add(new EpochBucket());
    }

    this.baseLine = newBaseLine;
  }

  /**
   * Given a block hash evicts a list of attestations made to that block.
   *
   * @param root a block root.
   * @return evicted attestations.
   */
  List<ReceivedAttestation> evict(Hash32 root) {
    if (!isInitialized()) {
      return Collections.emptyList();
    }

    List<ReceivedAttestation> evictedFromAllEpochs = new ArrayList<>();
    for (EpochBucket epoch : queue) {
      evictedFromAllEpochs.addAll(epoch.evict(root));
    }

    return evictedFromAllEpochs;
  }

  /**
   * Queues attestation.
   *
   * @param epoch target epoch.
   * @param root beacon block root.
   * @param attestation an attestation object.
   */
  void add(EpochNumber epoch, Hash32 root, ReceivedAttestation attestation) {
    if (!isInitialized() || epoch.less(baseLine)) {
      return;
    }

    EpochBucket epochBucket = getEpochBucket(epoch);
    epochBucket.add(root, attestation);

    purgeQueue();
  }

  /** Purges queue by its {@link #maxSize}. */
  private void purgeQueue() {
    for (EpochNumber e = baseLine;
        computeSize() > maxSize && e.less(baseLine.plus(trackedEpochs));
        e = e.increment()) {
      EpochBucket epochBucket = getEpochBucket(e);
      while (epochBucket.size() > 0 && computeSize() > maxSize) {
        epochBucket.removeEarliest();
      }
    }
  }

  /** @return a number of attestations in the queue. */
  private long computeSize() {
    return queue.stream().map(EpochBucket::size).reduce(0L, Long::sum);
  }

  /**
   * Returns an epoch bucket.
   *
   * @param epoch an epoch number.
   * @return a bucket corresponding to the epoch.
   */
  private EpochBucket getEpochBucket(EpochNumber epoch) {
    assert epoch.greaterEqual(baseLine);
    return queue.get(epoch.minus(baseLine).getIntValue());
  }

  /** @return {@code true} if {@link #baseLine} is defined, {@code false} otherwise. */
  private boolean isInitialized() {
    return baseLine != null;
  }

  /**
   * An epoch bucket.
   *
   * <p>Holds attestation with the same target epoch.
   */
  static final class EpochBucket {

    /** A map of beacon block root on a list of attestations made to that root. */
    private final Map<Hash32, LinkedList<RootBucketEntry>> bucket = new HashMap<>();
    /** An LRU index for the attestations. */
    private final LinkedList<LinkedList<RootBucketEntry>> lruIndex = new LinkedList<>();

    /** A number of attestations in the bucket. */
    private long size = 0;

    /**
     * Adds attestation to the bucket.
     *
     * @param root beacon block root.
     * @param attestation an attestation.
     */
    void add(Hash32 root, ReceivedAttestation attestation) {
      LinkedList<RootBucketEntry> rootBucket = getOrInsert(root);
      rootBucket.add(new RootBucketEntry(System.nanoTime(), attestation));
      updateIndex();
      size += 1;
    }

    private LinkedList<RootBucketEntry> getOrInsert(Hash32 root) {
      LinkedList<RootBucketEntry> rootBucket = bucket.get(root);
      if (rootBucket == null) {
        bucket.put(root, rootBucket = new LinkedList<>());
        lruIndex.add(rootBucket);
      }
      return rootBucket;
    }

    private void updateIndex() {
      lruIndex.sort(Comparator.comparing(b -> b.getFirst().timestamp));
    }

    /**
     * Given beacon block root evicts attestations made to that root.
     *
     * @param root beacon block root.
     * @return a list of evicted attestations.
     */
    List<ReceivedAttestation> evict(Hash32 root) {
      List<RootBucketEntry> evicted = bucket.remove(root);
      if (evicted != null) {
        size -= evicted.size();
        lruIndex.remove(evicted);
        updateIndex();
      }
      return evicted != null
          ? evicted.stream().map(RootBucketEntry::getAttestation).collect(Collectors.toList())
          : Collections.emptyList();
    }

    /**
     * Removes an earliest attestation from the bucket.
     *
     * @return removed attestation.
     */
    ReceivedAttestation removeEarliest() {
      if (size > 0) {
        LinkedList<RootBucketEntry> oldestBucket = lruIndex.getFirst();
        RootBucketEntry entry = oldestBucket.removeFirst();
        if (oldestBucket.isEmpty()) {
          lruIndex.removeFirst();
          bucket.values().remove(oldestBucket);
        }
        updateIndex();

        size -= 1;
        return entry.attestation;
      } else {
        return null;
      }
    }

    /** @return size of a bucket. */
    long size() {
      return size;
    }
  }

  /** Entry that stores LRU timestamp along with attestation. */
  static final class RootBucketEntry {

    /** Timestamp identifying a moment in time of when attestation was added to the queue. */
    private final long timestamp;
    /** An attestation itself. */
    private final ReceivedAttestation attestation;

    public RootBucketEntry(long timestamp, ReceivedAttestation attestation) {
      this.timestamp = timestamp;
      this.attestation = attestation;
    }

    ReceivedAttestation getAttestation() {
      return attestation;
    }
  }
}

package org.ethereum.beacon.chain.pool.churn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.EpochNumber;

final class ChurnQueue {

  private final long maxSize;
  private final LinkedList<Bucket> buckets = new LinkedList<>();

  private EpochNumber lowerEpoch;
  private EpochNumber upperEpoch;
  private long size = 0;

  public ChurnQueue(long maxSize) {
    this.maxSize = maxSize;
  }

  Stream<Attestation> stream() {
    return buckets.stream().map(Bucket::getAttestations).flatMap(List::stream);
  }

  void add(List<Attestation> attestation) {
    attestation.forEach(this::addImpl);
    // heavy operation, don't wanna run it after each added attestation
    purgeQueue();
  }

  private void addImpl(Attestation attestation) {
    EpochNumber epoch = attestation.getData().getTarget().getEpoch();
    if (epoch.greaterEqual(lowerEpoch) && epoch.lessEqual(upperEpoch)) {
      Bucket bucket = getOrCreateBucket(epoch);
      bucket.attestations.add(attestation);

      size += 1;
    }
  }

  void updateEpochBoundaries(EpochNumber lower, EpochNumber upper) {
    assert lower.lessEqual(upper);

    while (buckets.size() > 0 && buckets.getFirst().epoch.less(lower)) {
      buckets.removeFirst();
    }

    while (buckets.size() > 0 && buckets.getLast().epoch.greater(upper)) {
      buckets.removeLast();
    }

    this.lowerEpoch = lower;
    this.upperEpoch = upper;
  }

  Bucket getOrCreateBucket(EpochNumber epoch) {
    for (Bucket bucket : buckets) {
      if (bucket.epoch.equals(epoch)) {
        return bucket;
      }
    }

    Bucket newBucket = new Bucket(epoch);
    buckets.add(new Bucket(epoch));
    buckets.sort(Comparator.comparing(Bucket::getEpoch));

    return newBucket;
  }

  void purgeQueue() {
    if (maxSize > size) {
      // TODO calculate weights and sieve the attestations
    }
  }

  private static final class Bucket {
    private final EpochNumber epoch;
    private final List<Attestation> attestations = new ArrayList<>();

    Bucket(EpochNumber epoch) {
      this.epoch = epoch;
    }

    EpochNumber getEpoch() {
      return epoch;
    }

    List<Attestation> getAttestations() {
      return attestations;
    }
  }
}

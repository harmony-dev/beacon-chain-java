package org.ethereum.beacon.chain.pool.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.core.types.EpochNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;

final class Queue {
  private final LinkedList<EpochBucket> queue = new LinkedList<>();
  private final EpochNumber trackedEpochs;
  private final long maxSize;

  private EpochNumber baseLine;

  Queue(EpochNumber trackedEpochs, long maxSize) {
    assert maxSize > 0;
    assert trackedEpochs.getValue() > 0;

    this.trackedEpochs = trackedEpochs;
    this.maxSize = maxSize;
  }

  synchronized void moveBaseLine(EpochNumber newBaseLine) {
    assert baseLine == null || newBaseLine.greater(baseLine);

    for (long i = 0; i < newBaseLine.minus(baseLine).getValue() && queue.size() > 0; i++) {
      queue.removeFirst();
    }

    while (queue.size() < trackedEpochs.getValue()) {
      queue.add(new EpochBucket());
    }

    this.baseLine = newBaseLine;
  }

  synchronized List<ReceivedAttestation> evict(Hash32 root) {
    if (!isInitialized()) {
      return Collections.emptyList();
    }

    List<ReceivedAttestation> evictedFromAllEpochs = new ArrayList<>();
    for (EpochBucket epoch : queue) {
      evictedFromAllEpochs.addAll(epoch.evict(root));
    }

    return evictedFromAllEpochs;
  }

  synchronized void add(EpochNumber epoch, Hash32 root, ReceivedAttestation attestation) {
    if (!isInitialized() || epoch.less(baseLine)) {
      return;
    }

    EpochBucket epochBucket = getEpochBucket(epoch);
    epochBucket.add(root, attestation);

    purgeQueue();
  }

  private void purgeQueue() {
    for (EpochNumber e = baseLine;
        computeSize() > maxSize && e.less(baseLine.plus(trackedEpochs));
        e = e.increment()) {
      EpochBucket epochBucket = getEpochBucket(e);
      while (epochBucket.size() > 0 && computeSize() > maxSize) {
        epochBucket.removeOldest();
      }
    }
  }

  private long computeSize() {
    return queue.stream().map(EpochBucket::size).reduce(0L, Long::sum);
  }

  private EpochBucket getEpochBucket(EpochNumber epoch) {
    assert epoch.greaterEqual(baseLine);
    return queue.get(epoch.minus(baseLine).getIntValue());
  }

  private boolean isInitialized() {
    return baseLine != null;
  }

  static final class EpochBucket {
    private final Map<Hash32, LinkedList<RootBucketEntry>> bucket = new HashMap<>();
    private final LinkedList<LinkedList<RootBucketEntry>> lruIndex = new LinkedList<>();

    private long size = 0;

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

    ReceivedAttestation removeOldest() {
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

    long size() {
      return size;
    }
  }

  static final class RootBucketEntry {
    private final long timestamp;
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

package org.ethereum.beacon.core;

import org.ethereum.beacon.core.Incremental.UpdateTracker;

public interface Incremental<TrackerType extends UpdateTracker> {

  interface UpdateTracker {}

  interface ContainerUpdateTracker extends UpdateTracker {
    void elementUpdated(String fieldName);
  }

  interface VectorUpdateTracker extends UpdateTracker {
    void elementUpdated(long elementIndex);
  }

  interface ListUpdateTracker extends VectorUpdateTracker {
    void elementInserted(int position, int newSize);

    default void elementRemoved(int position, int newSize) {
      throw new UnsupportedOperationException();
    }
  }

  void installUpdateTracker(TrackerType updateTracker);
}

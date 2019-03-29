package org.ethereum.beacon.util;

import java.util.function.Supplier;
import org.ethereum.beacon.util.Incremental.UpdateTracker;

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

  <C extends TrackerType> C getUpdateTracker(Supplier<C> trackerFactory);
}

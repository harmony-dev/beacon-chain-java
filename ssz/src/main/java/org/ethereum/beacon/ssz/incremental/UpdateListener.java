package org.ethereum.beacon.ssz.incremental;

/**
 * A listener which is notified on changes in {@link ObservableComposite}
 */
public interface UpdateListener {

  /**
   * Notifies that the child with specified index was updated, removed or added.
   * E.g. if an element with index 3 is removed from a list of size 5, then this method
   * is expected to be called 3 times with values <code>3, 4, 5</code>
   */
  void childUpdated(int childIndex);

  /**
   * Creates an independent copy of this {@link UpdateListener} which will be tracking
   * updates independently of this listener updates.
   * This is done to support {@link ObservableComposite} instances copying.
   * When forking both {@link UpdateListener} copies should have the same changes accumulated
   * before the fork.
   */
  UpdateListener fork();
}

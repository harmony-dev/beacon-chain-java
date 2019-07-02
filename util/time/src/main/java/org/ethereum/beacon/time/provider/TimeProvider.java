package org.ethereum.beacon.time.provider;

import org.ethereum.beacon.core.types.Time;
import org.reactivestreams.Publisher;

/**
 * Supplier of Time ticks
 */
public interface TimeProvider {
  Publisher<Time> getTimeStream();
}

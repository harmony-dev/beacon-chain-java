package org.ethereum.beacon.time;

import org.ethereum.beacon.core.types.Time;
import org.reactivestreams.Publisher;

/**
 * High level time supplier, uses several {@link org.ethereum.beacon.time.provider.TimeProvider} to
 * produce result time
 */
public interface TimeStrategy {
  Publisher<Time> getTimeStream();
}

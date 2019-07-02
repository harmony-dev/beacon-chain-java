package org.ethereum.beacon.time.mapper;

import org.ethereum.beacon.core.types.Time;
import org.reactivestreams.Publisher;

/** Maps stream of objects with some kind of time info to time stream */
public interface TimeMapper {
  Publisher<Time> getTimeStream();
}

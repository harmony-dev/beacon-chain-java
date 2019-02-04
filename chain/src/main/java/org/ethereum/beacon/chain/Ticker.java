package org.ethereum.beacon.chain;

import org.reactivestreams.Publisher;

public interface Ticker<T> {
  void start();

  Publisher<T> getTickerStream();
}

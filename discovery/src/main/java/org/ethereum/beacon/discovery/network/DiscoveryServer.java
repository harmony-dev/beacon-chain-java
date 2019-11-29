package org.ethereum.beacon.discovery.network;

import org.ethereum.beacon.schedulers.Scheduler;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Discovery server which listens to incoming messages according to setup */
public interface DiscoveryServer {
  void start(Scheduler scheduler);

  void stop();

  /** Raw incoming packets stream */
  Publisher<BytesValue> getIncomingPackets();
}

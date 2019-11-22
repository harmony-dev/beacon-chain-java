package org.ethereum.beacon.discovery.pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Container for any kind of objects used in packet-messages-tasks flow */
public class Envelope {
  private UUID id;
  private Map<Field, Object> data = new HashMap<>();

  public Envelope() {
    this.id = UUID.randomUUID();
  }

  public synchronized void put(Field key, Object value) {
    data.put(key, value);
  }

  public synchronized Object get(Field key) {
    return data.get(key);
  }

  public synchronized boolean remove(Field key) {
    return data.remove(key) != null;
  }

  public synchronized boolean contains(Field key) {
    return data.containsKey(key);
  }

  public UUID getId() {
    return id;
  }
}

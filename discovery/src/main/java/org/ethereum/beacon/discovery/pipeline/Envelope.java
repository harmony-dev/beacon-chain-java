package org.ethereum.beacon.discovery.pipeline;

import java.util.HashMap;
import java.util.Map;

public class Envelope {
  private Map<String, Object> data = new HashMap<>();

  public synchronized void put(String key, Object value) {
    data.put(key, value);
  }

  public synchronized Object get(String key) {
    return data.get(key);
  }

  public synchronized boolean remove(String key) {
    return data.remove(key) != null;
  }

  public synchronized boolean contains(String key) {
    return data.containsKey(key);
  }
}

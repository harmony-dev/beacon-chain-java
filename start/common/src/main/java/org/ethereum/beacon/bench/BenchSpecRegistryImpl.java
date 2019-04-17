package org.ethereum.beacon.bench;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.ethereum.beacon.consensus.BeaconChainSpec;

public class BenchSpecRegistryImpl implements BenchSpecRegistry {

  private final Map<String, BenchSpecWrapper> benchSpecs = new ConcurrentHashMap<>();

  @Override
  public BeaconChainSpec register(String name, BeaconChainSpec spec) {
    return benchSpecs.computeIfAbsent(name, s -> new BenchSpecWrapper(spec));
  }

  @Override
  public Optional<BenchSpecWrapper> get(String name) {
    return Optional.ofNullable(benchSpecs.get(name));
  }

  @Override
  public void startTracking() {
    benchSpecs.values().forEach(BenchSpecWrapper::startTracking);
  }
}

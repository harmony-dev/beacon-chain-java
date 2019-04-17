package org.ethereum.beacon.bench;

import java.util.Optional;
import org.ethereum.beacon.consensus.BeaconChainSpec;

public interface BenchSpecRegistry {

  BenchSpecRegistry NO_BENCHES =
      new BenchSpecRegistry() {
        @Override
        public BeaconChainSpec register(String name, BeaconChainSpec spec) {
          return spec;
        }

        @Override
        public Optional<BenchSpecWrapper> get(String name) {
          return Optional.empty();
        }

        @Override
        public void startTracking() {}
      };

  String EPOCH_BENCH = "epoch";
  String BLOCK_BENCH = "block";
  String SLOT_BENCH = "slot";

  BeaconChainSpec register(String name, BeaconChainSpec spec);

  Optional<BenchSpecWrapper> get(String name);

  void startTracking();

  static BenchSpecRegistry newInstance() {
    return new BenchSpecRegistryImpl();
  }
}

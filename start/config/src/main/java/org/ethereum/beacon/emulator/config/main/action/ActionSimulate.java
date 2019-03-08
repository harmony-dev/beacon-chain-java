package org.ethereum.beacon.emulator.config.main.action;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.emulator.config.simulator.PeersConfig;

/** Settings for validator simulation of several random validators */
public class ActionSimulate extends Action {
  private List<PeersConfig> peersConfigs = new ArrayList<>();

  public List<PeersConfig> getPeersConfigs() {
    return peersConfigs;
  }

  public void setPeersConfigs(List<PeersConfig> peersConfigs) {
    this.peersConfigs = new ArrayList<>(peersConfigs);
  }

  public void addPeersConfig(PeersConfig peersConfig) {
    peersConfigs.add(peersConfig);
  }
}

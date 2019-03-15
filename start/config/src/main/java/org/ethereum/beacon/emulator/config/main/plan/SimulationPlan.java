package org.ethereum.beacon.emulator.config.main.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.emulator.config.Config;
import org.ethereum.beacon.emulator.config.YamlPrinter;
import org.ethereum.beacon.emulator.config.simulator.PeersConfig;

public class SimulationPlan extends Plan implements Config {

  private List<PeersConfig> peers = new ArrayList<>();

  @JsonProperty("genesis-time")
  private int genesisTime = 600;

  private long seed = System.currentTimeMillis();

  public List<PeersConfig> getPeers() {
    return peers;
  }

  public void setPeers(List<PeersConfig> peers) {
    this.peers = new ArrayList<>(peers);
  }

  public void addPeers(PeersConfig peersConfig) {
    peers.add(peersConfig);
  }

  public int getGenesisTime() {
    return genesisTime;
  }

  public void setGenesisTime(int genesisTime) {
    this.genesisTime = genesisTime;
  }

  public long getSeed() {
    return seed;
  }

  public void setSeed(long seed) {
    this.seed = seed;
  }

  @Override
  public String toString() {
    return new YamlPrinter(this).getString();
  }
}

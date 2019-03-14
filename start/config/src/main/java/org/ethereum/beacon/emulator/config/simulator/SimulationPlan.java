package org.ethereum.beacon.emulator.config.simulator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.ethereum.beacon.emulator.config.Config;
import org.ethereum.beacon.emulator.config.YamlPrinter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationPlan implements Config {
  private List<PeersConfig> peers;

  @JsonProperty("bls-verify")
  private boolean blsVerifyEnabled = true;

  @JsonProperty("bls-sign")
  private boolean blsSignEnabled = true;

  @JsonProperty("genesis-time")
  private int genesisTime = 600;

  private long seed = System.currentTimeMillis();

  public List<PeersConfig> getPeers() {
    return peers;
  }

  public void setPeers(List<PeersConfig> peers) {
    this.peers = peers;
  }

  public boolean isBlsVerifyEnabled() {
    return blsVerifyEnabled;
  }

  public void setBlsVerifyEnabled(boolean blsVerifyEnabled) {
    this.blsVerifyEnabled = blsVerifyEnabled;
  }

  public boolean isBlsSignEnabled() {
    return blsSignEnabled;
  }

  public void setBlsSignEnabled(boolean blsSignEnabled) {
    this.blsSignEnabled = blsSignEnabled;
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

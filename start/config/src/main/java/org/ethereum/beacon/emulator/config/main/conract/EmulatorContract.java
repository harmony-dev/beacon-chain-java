package org.ethereum.beacon.emulator.config.main.conract;

import java.util.List;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys;

public class EmulatorContract extends Contract {
  Long genesisTime;
  List<ValidatorKeys> keys;

  public Long getGenesisTime() {
    return genesisTime;
  }

  public void setGenesisTime(Long genesisTime) {
    this.genesisTime = genesisTime;
  }

  public List<ValidatorKeys> getKeys() {
    return keys;
  }

  public void setKeys(List<ValidatorKeys> keys) {
    this.keys = keys;
  }
}

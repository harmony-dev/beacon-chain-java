package org.ethereum.beacon.emulator.config.main.conract;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys;

public class EmulatorContract extends Contract {

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT")
  private Date genesisTime;

  private Integer balance;
  private List<ValidatorKeys> keys;

  public Date getGenesisTime() {
    return genesisTime;
  }

  public void setGenesisTime(Date genesisTime) {
    this.genesisTime = genesisTime;
  }

  public Integer getBalance() {
    return balance;
  }

  public void setBalance(Integer balance) {
    this.balance = balance;
  }

  public List<ValidatorKeys> getKeys() {
    return keys;
  }

  public void setKeys(List<ValidatorKeys> keys) {
    this.keys = keys;
  }
}

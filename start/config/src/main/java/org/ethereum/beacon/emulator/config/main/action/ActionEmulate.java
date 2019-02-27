package org.ethereum.beacon.emulator.config.main.action;

import java.util.List;

/** Settings for validator emulation of several random validators */
public class ActionEmulate extends Action {
  private Integer count;
  private List<String> privateKeys;

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public List<String> getPrivateKeys() {
    return privateKeys;
  }

  public void setPrivateKeys(List<String> privateKeys) {
    this.privateKeys = privateKeys;
  }
}

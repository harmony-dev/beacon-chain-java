package org.ethereum.beacon.emulator.config.data.v1.action;

/** Settings for validator emulation of several random validators */
public class ActionEmulate extends Action {
  private int count;

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }
}

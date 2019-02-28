package org.ethereum.beacon.emulator.config.main;

import org.ethereum.beacon.emulator.config.main.action.Action;

import java.util.List;

/** Configuration of application tasks */
public class Plan {
  private List<Action> sync;
  private List<Action> validator;

  public List<Action> getSync() {
    return sync;
  }

  public void setSync(List<Action> sync) {
    this.sync = sync;
  }

  public List<Action> getValidator() {
    return validator;
  }

  public void setValidator(List<Action> validator) {
    this.validator = validator;
  }
}

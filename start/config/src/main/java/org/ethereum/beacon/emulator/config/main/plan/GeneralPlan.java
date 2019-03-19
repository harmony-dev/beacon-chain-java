package org.ethereum.beacon.emulator.config.main.plan;

import java.util.List;
import org.ethereum.beacon.emulator.config.main.action.Action;

public class GeneralPlan extends Plan {
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

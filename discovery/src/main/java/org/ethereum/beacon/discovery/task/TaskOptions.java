package org.ethereum.beacon.discovery.task;

public class TaskOptions {
  private boolean livenessUpdate;

  public TaskOptions(boolean livenessUpdate) {
    this.livenessUpdate = livenessUpdate;
  }

  public boolean isLivenessUpdate() {
    return livenessUpdate;
  }
}

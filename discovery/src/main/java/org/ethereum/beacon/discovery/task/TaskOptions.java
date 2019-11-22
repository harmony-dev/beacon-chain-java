package org.ethereum.beacon.discovery.task;

/** Specific options to clarify task features */
public class TaskOptions {
  private boolean livenessUpdate;
  private int distance;

  public TaskOptions(boolean livenessUpdate) {
    this.livenessUpdate = livenessUpdate;
  }

  public TaskOptions(boolean livenessUpdate, int distance) {
    this.livenessUpdate = livenessUpdate;
    this.distance = distance;
  }

  public boolean isLivenessUpdate() {
    return livenessUpdate;
  }

  public int getDistance() {
    return distance;
  }
}

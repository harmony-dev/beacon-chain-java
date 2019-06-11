package org.ethereum.beacon.validator.api.model;

public class TimeResponse {
  private Long time;

  public TimeResponse() {
  }

  public TimeResponse(Long time) {
    this.time = time;
  }

  public Long getTime() {
    return time;
  }

  public void setTime(Long time) {
    this.time = time;
  }
}

package org.ethereum.beacon.emulator.config.simulator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties(ignoreUnknown = false)
public class Peer {
  private int count = 1;
  private boolean validator = true;

  @JsonIgnore
  private long systemTimeShift = 0;
  @JsonIgnore
  private long wireInboundDelay = 0;
  @JsonIgnore
  private long wireOutboundDelay = 0;

  @JsonIgnore
  private String blsPrivateKey = null;

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public boolean isValidator() {
    return validator;
  }

  public void setValidator(boolean validator) {
    this.validator = validator;
  }

  public long getSystemTimeShift() {
    return systemTimeShift;
  }

  public void setSystemTimeShift(long systemTimeShift) {
    this.systemTimeShift = systemTimeShift;
  }

  public long getWireInboundDelay() {
    return wireInboundDelay;
  }

  public void setWireInboundDelay(long wireInboundDelay) {
    this.wireInboundDelay = wireInboundDelay;
  }

  public long getWireOutboundDelay() {
    return wireOutboundDelay;
  }

  public void setWireOutboundDelay(long wireOutboundDelay) {
    this.wireOutboundDelay = wireOutboundDelay;
  }

  public String getBlsPrivateKey() {
    return blsPrivateKey;
  }

  public void setBlsPrivateKey(String blsPrivateKey) {
    this.blsPrivateKey = blsPrivateKey;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("count", count)
        .add("validator", validator)
        .add("systemTimeShift", systemTimeShift)
        .add("wireInboundDelay", wireInboundDelay)
        .add("wireOutboundDelay", wireOutboundDelay)
        .add("blsPrivateKey", blsPrivateKey)
        .toString();
  }
}

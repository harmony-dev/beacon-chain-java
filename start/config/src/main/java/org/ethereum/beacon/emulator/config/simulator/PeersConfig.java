package org.ethereum.beacon.emulator.config.simulator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PeersConfig {
  private int count = 1;
  private boolean validator = true;

  private long systemTimeShift = 0;
  private long wireInboundDelay = 0;
  private long wireOutboundDelay = 0;

  private String blsPrivateKey = null;

  public PeersConfig(
      int count,
      boolean validator,
      long systemTimeShift,
      long wireInboundDelay,
      long wireOutboundDelay,
      String blsPrivateKey) {
    this.count = count;
    this.validator = validator;
    this.systemTimeShift = systemTimeShift;
    this.wireInboundDelay = wireInboundDelay;
    this.wireOutboundDelay = wireOutboundDelay;
    this.blsPrivateKey = blsPrivateKey;
  }

  public PeersConfig() {
  }

  public PeersConfig(int count, boolean validator) {
    this.count = count;
    this.validator = validator;
  }

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
}

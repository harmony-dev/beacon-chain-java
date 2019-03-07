package org.ethereum.beacon.emulator.config.simulator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PeersConfig {
  private int peersCount = 1;
  private boolean isValidator = true;

  private long systemTimeShift = 0;
  private long wireInboundDelay = 0;
  private long wireOutboundDelay = 0;

  private String blsPrivateKey = null;

  public int getPeersCount() {
    return peersCount;
  }

  public void setPeersCount(int peersCount) {
    this.peersCount = peersCount;
  }

  public boolean isValidator() {
    return isValidator;
  }

  public void setValidator(boolean validator) {
    isValidator = validator;
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

package org.ethereum.beacon.validator.api.model;

public class VersionResponse {
  private String version;

  public VersionResponse() {
  }

  public VersionResponse(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}

package org.ethereum.beacon.validator.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigInteger;

public class ForkResponse {
  private ForkInfo fork;

  @JsonProperty("chain_id")
  private BigInteger chainId;

  public ForkResponse() {}

  public ForkResponse(
      String currentVersion, String previousVersion, Long epoch, BigInteger chainId) {
    ForkInfo fork = new ForkInfo();
    fork.setEpoch(epoch);
    fork.setCurrentVersion(currentVersion);
    fork.setPreviousVersion(previousVersion);
    this.fork = fork;
    this.chainId = chainId;
  }

  public ForkInfo getFork() {
    return fork;
  }

  public void setFork(ForkInfo fork) {
    this.fork = fork;
  }

  public BigInteger getChainId() {
    return chainId;
  }

  public void setChainId(BigInteger chainId) {
    this.chainId = chainId;
  }

  public static class ForkInfo {
    @JsonProperty("current_version")
    private String currentVersion;

    @JsonProperty("previous_version")
    private String previousVersion;

    private Long epoch;

    public String getCurrentVersion() {
      return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
      this.currentVersion = currentVersion;
    }

    public String getPreviousVersion() {
      return previousVersion;
    }

    public void setPreviousVersion(String previousVersion) {
      this.previousVersion = previousVersion;
    }

    public Long getEpoch() {
      return epoch;
    }

    public void setEpoch(Long epoch) {
      this.epoch = epoch;
    }
  }
}

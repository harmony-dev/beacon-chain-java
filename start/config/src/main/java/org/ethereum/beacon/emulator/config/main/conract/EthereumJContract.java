package org.ethereum.beacon.emulator.config.main.conract;

public class EthereumJContract extends Contract {
  private String contractAddress;
  private long contractBlock;
  private String contractAbiPath;

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public long getContractBlock() {
    return contractBlock;
  }

  public void setContractBlock(long contractBlock) {
    this.contractBlock = contractBlock;
  }

  public String getContractAbiPath() {
    return contractAbiPath;
  }

  public void setContractAbiPath(String contractAbiPath) {
    this.contractAbiPath = contractAbiPath;
  }
}

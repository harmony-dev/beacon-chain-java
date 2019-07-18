package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.type.BlsSignedTestCase;
import org.ethereum.beacon.test.type.NamedTestCase;

import java.util.List;
import java.util.stream.Collectors;

public class GenesisInitTestCase implements NamedTestCase, BlsSignedTestCase {
  private String description;
  //  the root of the Eth-1 block, hex encoded, with prefix 0x
  @JsonProperty("eth1_block_hash")
  private String eth1BlockHash;
  //  the timestamp of the block, in seconds.
  @JsonProperty("eth1_timestamp")
  private Long eth1Timestamp;

  private List<StateTestCase.BlockData.BlockBodyData.DepositData> deposits;
  private StateTestCase.BeaconStateData state;

  @JsonProperty("bls_setting")
  private Integer blsSetting;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getName() {
    return getDescription();
  }

  public StateTestCase.BeaconStateData getState() {
    return state;
  }

  public void setState(StateTestCase.BeaconStateData state) {
    this.state = state;
  }

  @Override
  public Integer getBlsSetting() {
    return blsSetting;
  }

  public void setBlsSetting(Integer blsSetting) {
    this.blsSetting = blsSetting;
  }

  public String getEth1BlockHash() {
    return eth1BlockHash;
  }

  public void setEth1BlockHash(String eth1BlockHash) {
    this.eth1BlockHash = eth1BlockHash;
  }

  public Long getEth1Timestamp() {
    return eth1Timestamp;
  }

  public void setEth1Timestamp(Long eth1Timestamp) {
    this.eth1Timestamp = eth1Timestamp;
  }

  public List<StateTestCase.BlockData.BlockBodyData.DepositData> getDeposits() {
    return deposits;
  }

  public void setDeposits(List<StateTestCase.BlockData.BlockBodyData.DepositData> deposits) {
    this.deposits = deposits;
  }

  public List<Deposit> getDepositList() {
    return getDeposits().stream().map(StateTestUtils::parseDeposit).collect(Collectors.toList());
  }
}

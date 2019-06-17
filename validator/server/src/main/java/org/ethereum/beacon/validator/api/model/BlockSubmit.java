package org.ethereum.beacon.validator.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BlockSubmit {
  @JsonProperty("beacon_block")
  private BlockData beaconBlock;

  public BlockSubmit() {
  }

  public BlockSubmit(BlockData beaconBlock) {
    this.beaconBlock = beaconBlock;
  }

  public BlockData getBeaconBlock() {
    return beaconBlock;
  }

  public void setBeaconBlock(BlockData beaconBlock) {
    this.beaconBlock = beaconBlock;
  }
}

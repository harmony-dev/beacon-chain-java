package org.ethereum.beacon.validator.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.validator.api.convert.BeaconBlockConverter;

public class BlockSubmit {
  @JsonProperty("beacon_block")
  private BlockData beaconBlock;

  public BlockSubmit() {}

  public BlockSubmit(BlockData beaconBlock) {
    this.beaconBlock = beaconBlock;
  }

  public static BlockSubmit fromBeaconBlock(SignedBeaconBlock block) {
    return new BlockSubmit(BeaconBlockConverter.serialize(block));
  }

  public BlockData getBeaconBlock() {
    return beaconBlock;
  }

  public void setBeaconBlock(BlockData beaconBlock) {
    this.beaconBlock = beaconBlock;
  }

  public SignedBeaconBlock createBeaconBlock(SpecConstants constants) {
    return BeaconBlockConverter.deserializeAsSigned(beaconBlock, constants);
  }
}

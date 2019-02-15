package org.ethereum.beacon.emulator.config.data.v1.chainspec;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.HonestValidatorParameters;

public class HonestValidatorParametersData implements HonestValidatorParameters {

  @JsonProperty("ETH1_FOLLOW_DISTANCE")
  private Long ETH1_FOLLOW_DISTANCE;

  @Override
  public long getEth1FollowDistance() {
    return getETH1_FOLLOW_DISTANCE();
  }

  public Long getETH1_FOLLOW_DISTANCE() {
    return ETH1_FOLLOW_DISTANCE;
  }

  public void setETH1_FOLLOW_DISTANCE(Long ETH1_FOLLOW_DISTANCE) {
    this.ETH1_FOLLOW_DISTANCE = ETH1_FOLLOW_DISTANCE;
  }
}

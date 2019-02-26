package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.spec.HonestValidatorParameters;

public class HonestValidatorParametersData implements HonestValidatorParameters {

  @JsonProperty("ETH1_FOLLOW_DISTANCE")
  private Long ETH1_FOLLOW_DISTANCE;

  @Override
  @JsonIgnore
  public long getEth1FollowDistance() {
    return getETH1_FOLLOW_DISTANCE();
  }

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  public Long getETH1_FOLLOW_DISTANCE() {
    return ETH1_FOLLOW_DISTANCE;
  }

  public void setETH1_FOLLOW_DISTANCE(Long ETH1_FOLLOW_DISTANCE) {
    this.ETH1_FOLLOW_DISTANCE = ETH1_FOLLOW_DISTANCE;
  }
}

package org.ethereum.beacon.test.type.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.test.type.BlsSignedTestCase;
import org.ethereum.beacon.test.type.NamedTestCase;

import static org.ethereum.beacon.test.StateTestUtils.parseBeaconState;

public class GenesisValidityTestCase implements NamedTestCase, BlsSignedTestCase {
  private String description;
  private StateTestCase.BeaconStateData genesis;

  @JsonProperty("is_valid")
  private boolean isValid;

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

  @Override
  public Integer getBlsSetting() {
    return blsSetting;
  }

  public void setBlsSetting(Integer blsSetting) {
    this.blsSetting = blsSetting;
  }

  public StateTestCase.BeaconStateData getGenesis() {
    return genesis;
  }

  public void setGenesis(StateTestCase.BeaconStateData genesis) {
    this.genesis = genesis;
  }

  public BeaconState getGenesisState(SpecConstants specConstants) {
    return parseBeaconState(specConstants, getGenesis());
  }

  public boolean isValid() {
    return isValid;
  }

  public void setValid(boolean valid) {
    isValid = valid;
  }
}

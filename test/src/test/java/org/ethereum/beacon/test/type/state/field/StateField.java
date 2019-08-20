package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.type.model.BeaconStateData;

public interface StateField extends DataMapperAccessor {
  default BeaconState getState(SpecConstants constants) {
    final String key = "state.yaml";
    try {
      if (getFiles().containsKey(key)) {
        BeaconStateData stateData =
            getMapper().readValue(getFiles().get(key), BeaconStateData.class);
        return StateTestUtils.parseBeaconState(constants, stateData);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`state` not defined");
  }
}

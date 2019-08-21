package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.type.model.BeaconStateData;

public interface PreField extends DataMapperAccessor {
  default BeaconState getPre(SpecConstants constants) {
    final String key = useSszWhenPossible() ? "pre.ssz" : "pre.yaml";

    // SSZ
    if (useSszWhenPossible()) {
      return getSszSerializer().decode(getFiles().get(key), BeaconStateImpl.class);
    }

    // YAML
    try {
      if (getFiles().containsKey(key)) {
        BeaconStateData stateData =
            getMapper().readValue(getFiles().get(key).extractArray(), BeaconStateData.class);
        return StateTestUtils.parseBeaconState(constants, stateData);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`pre` not defined");
  }
}

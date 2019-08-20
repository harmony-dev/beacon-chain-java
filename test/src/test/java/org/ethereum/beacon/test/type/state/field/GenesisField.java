package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.test.type.model.BeaconStateData;

import static org.ethereum.beacon.test.StateTestUtils.parseBeaconState;

public interface GenesisField extends DataMapperAccessor {
  default BeaconState getGenesis(SpecConstants constants) {
    final String key = "genesis.yaml";
    try {
      if (getFiles().containsKey(key)) {
        BeaconStateData genesisData =
            getMapper().readValue(getFiles().get(key), BeaconStateData.class);
        return parseBeaconState(constants, genesisData);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`genesis` not defined");
  }
}

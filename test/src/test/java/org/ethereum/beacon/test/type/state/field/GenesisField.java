package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.test.type.DataMapperAccessor;
import org.ethereum.beacon.test.type.model.BeaconStateData;

import static org.ethereum.beacon.test.StateTestUtils.parseBeaconState;

public interface GenesisField extends DataMapperAccessor {
  default BeaconState getGenesis(SpecConstants constants) {
    final String key = useSszWhenPossible() ? "genesis.ssz" : "genesis.yaml";

    // SSZ
    if (useSszWhenPossible()) {
      return getSszSerializer().decode(getFiles().get(key), BeaconStateImpl.class);
    }

    // YAML
    try {
      if (getFiles().containsKey(key)) {
        BeaconStateData genesisData =
            getMapper().readValue(getFiles().get(key).extractArray(), BeaconStateData.class);
        return parseBeaconState(constants, genesisData);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`genesis` not defined");
  }
}

package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.type.model.BeaconStateData;

public interface PostField extends DataMapperAccessor {
  default BeaconState getPost(SpecConstants constants) {
    final String key = useSszWhenPossible() ? "post.ssz" : "post.yaml";

    if (getFiles().containsKey(key)) {
      // SSZ
      if (useSszWhenPossible()) {
        return getSszSerializer().decode(getFiles().get(key), BeaconStateImpl.class);
      } else { // YAML
        try {
          BeaconStateData stateData =
              getMapper().readValue(getFiles().get(key).extractArray(), BeaconStateData.class);
          return StateTestUtils.parseBeaconState(constants, stateData);

        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    }

    return null; // XXX: optional field
  }
}

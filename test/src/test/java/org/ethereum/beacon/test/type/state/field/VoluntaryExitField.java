package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.test.type.model.BlockData;

import static org.ethereum.beacon.test.StateTestUtils.parseVoluntaryExit;

public interface VoluntaryExitField extends DataMapperAccessor {
  default VoluntaryExit getVoluntaryExit() {
    final String key = useSszWhenPossible() ? "voluntary_exit.ssz" : "voluntary_exit.yaml";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`voluntary_exit` not defined");
    }

    // SSZ
    if (useSszWhenPossible()) {
      return getSszSerializer().decode(getFiles().get(key), VoluntaryExit.class);
    }

    // YAML
    try {
      BlockData.BlockBodyData.VoluntaryExitData voluntaryExitData =
          getMapper()
              .readValue(getFiles().get(key).extractArray(), BlockData.BlockBodyData.VoluntaryExitData.class);
      return parseVoluntaryExit(voluntaryExitData);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

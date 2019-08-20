package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.test.type.model.BlockData;

import static org.ethereum.beacon.test.StateTestUtils.parseVoluntaryExit;

public interface VoluntaryExitField extends DataMapperAccessor {
  default VoluntaryExit getVoluntaryExit() {
    final String key = "voluntary_exit.yaml";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`voluntary_exit` not defined");
    }

    try {
      BlockData.BlockBodyData.VoluntaryExitData voluntaryExitData =
          getMapper()
              .readValue(getFiles().get(key), BlockData.BlockBodyData.VoluntaryExitData.class);
      return parseVoluntaryExit(voluntaryExitData);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

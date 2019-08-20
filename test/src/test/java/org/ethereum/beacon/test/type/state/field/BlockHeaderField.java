package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.test.type.model.BlockData;

import static org.ethereum.beacon.test.StateTestUtils.parseBlockData;

public interface BlockHeaderField extends DataMapperAccessor {
  default BeaconBlock getBlock(SpecConstants constants) {
    final String key = "block.yaml";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`block` not defined");
    }

    try {
      BlockData blockData = getMapper().readValue(getFiles().get(key), BlockData.class);
      return parseBlockData(blockData, constants);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

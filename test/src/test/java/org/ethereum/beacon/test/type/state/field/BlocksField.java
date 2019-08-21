package org.ethereum.beacon.test.type.state.field;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.test.type.model.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.ethereum.beacon.test.StateTestUtils.parseBlockData;

public interface BlocksField extends DataMapperAccessor {
  default List<BeaconBlock> getBlocks(SpecConstants constants) {
    final Function<Integer, String> blockKey =
        useSszWhenPossible()
            ? (n) -> String.format("blocks_%d.ssz", n)
            : (n) -> String.format("blocks_%d.yaml", n);
    final String metaKey = "meta.yaml";
    try {
      if (getFiles().containsKey(metaKey)) {
        List<BeaconBlock> blocks = new ArrayList<>();
        Integer blocksCount =
            getMapper()
                .readValue(getFiles().get(metaKey).extractArray(), MetaClass.class)
                .getBlocksCount();
        for (int i = 0; i < blocksCount; ++i) {
          // SSZ
          if (useSszWhenPossible()) {
            blocks.add(
                getSszSerializer().decode(getFiles().get(blockKey.apply(i)), BeaconBlock.class));
          } else { // YAML
            BlockData blockData =
                getMapper()
                    .readValue(getFiles().get(blockKey.apply(i)).extractArray(), BlockData.class);
            blocks.add(parseBlockData(blockData, constants));
          }
        }
        return blocks;
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`blocks` not defined");
  }

  class MetaClass {
    @JsonProperty("blocks_count")
    private Integer blocksCount;

    public Integer getBlocksCount() {
      return blocksCount;
    }

    public void setBlocksCount(Integer blocksCount) {
      this.blocksCount = blocksCount;
    }
  }
}

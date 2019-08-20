package org.ethereum.beacon.test.type.shuffle;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ethereum.beacon.test.type.state.DataMapperTestCase;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Shuffling test case <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/shuffling">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/shuffling</a>
 */
public class ShuffleTestCase extends DataMapperTestCase {
  private final Data delegate;

  public ShuffleTestCase(Map<String, String> files, ObjectMapper objectMapper, String description) {
    super(files, objectMapper, description);
    assert files.size() == 1;
    try {
      this.delegate = objectMapper.readValue(files.values().iterator().next(), Data.class);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read data", e);
    }
  }

  public String getSeed() {
    return delegate.seed;
  }

  public Integer getCount() {
    return delegate.count;
  }

  public List<Integer> getMapping() {
    return delegate.mapping;
  }

  @Override
  public String toString() {
    return "ShuffleTestCase{"
        + "seed='"
        + getSeed()
        + '\''
        + ", count="
        + getCount()
        + ", shuffled="
        + getMapping()
        + '}';
  }

  public static class Data {
    private String seed;
    private Integer count;
    private List<Integer> mapping;

    public String getSeed() {
      return seed;
    }

    public void setSeed(String seed) {
      this.seed = seed;
    }

    public Integer getCount() {
      return count;
    }

    public void setCount(Integer count) {
      this.count = count;
    }

    public List<Integer> getMapping() {
      return mapping;
    }

    public void setMapping(List<Integer> mapping) {
      this.mapping = mapping;
    }
  }
}

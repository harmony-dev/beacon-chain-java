package org.ethereum.beacon.test.type.shuffle;

import org.ethereum.beacon.test.type.TestCase;

import java.util.List;

/**
 * Shuffling test case <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/shuffling">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/shuffling</a>
 */
public class ShuffleTestCase implements TestCase {
  private String seed;
  private Integer count;
  private List<Integer> shuffled;

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

  public List<Integer> getShuffled() {
    return shuffled;
  }

  public void setShuffled(List<Integer> shuffled) {
    this.shuffled = shuffled;
  }

  @Override
  public String toString() {
    return "ShuffleTestCase{"
        + "seed='"
        + seed
        + '\''
        + ", count="
        + count
        + ", shuffled="
        + shuffled
        + '}';
  }
}

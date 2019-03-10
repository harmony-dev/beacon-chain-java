package org.ethereum.beacon.test.type;

import java.util.List;

/**
 * Shuffling test <a
 * href="https://github.com/ethereum/eth2.0-test-generators/tree/master/shuffling">https://github.com/ethereum/eth2.0-test-generators/tree/master/shuffling</a>
 */
public class ShuffleTestCase implements TestCase {
  private ShuffleInput input;
  private List<List<Integer>> output;
  private String seed;

  public ShuffleInput getInput() {
    return input;
  }

  public void setInput(ShuffleInput input) {
    this.input = input;
  }

  public List<List<Integer>> getOutput() {
    return output;
  }

  public void setOutput(List<List<Integer>> output) {
    this.output = output;
  }

  public String getSeed() {
    return seed;
  }

  public void setSeed(String seed) {
    this.seed = seed;
  }

  @Override
  public String toString() {
    return "ShuffleTestCase{"
        + "input="
        + input
        + ", output=[...]"
        + ", seed='"
        + seed
        + '\''
        + '}';
  }

  public static class ShuffleInput {
    private String epoch;
    private List<ShuffleTestValidator> validators;

    public String getEpoch() {
      return epoch;
    }

    public void setEpoch(String epoch) {
      this.epoch = epoch;
    }

    public List<ShuffleTestValidator> getValidators() {
      return validators;
    }

    public void setValidators(List<ShuffleTestValidator> validators) {
      this.validators = validators;
    }

    @Override
    public String toString() {
      return "Input{"
          + "epoch='"
          + epoch
          + '\''
          + ", validators="
          + (validators.isEmpty() ? "" : validators.get(0))
          + " (total "
          + validators.size()
          + " validators)"
          + '}';
    }

    public static class ShuffleTestValidator {
      private String activation_epoch;
      private String exit_epoch;
      private Integer original_index;

      public String getActivation_epoch() {
        return activation_epoch;
      }

      public void setActivation_epoch(String activation_epoch) {
        this.activation_epoch = activation_epoch;
      }

      public String getExit_epoch() {
        return exit_epoch;
      }

      public void setExit_epoch(String exit_epoch) {
        this.exit_epoch = exit_epoch;
      }

      public Integer getOriginal_index() {
        return original_index;
      }

      public void setOriginal_index(Integer original_index) {
        this.original_index = original_index;
      }

      @Override
      public String toString() {
        return "Validator{"
            + "activation_epoch='"
            + activation_epoch
            + '\''
            + ", exit_epoch='"
            + exit_epoch
            + '\''
            + ", original_index="
            + original_index
            + '}';
      }
    }
  }
}

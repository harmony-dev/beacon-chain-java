package org.ethereum.beacon.test.type.bls;

import org.ethereum.beacon.test.type.TestCase;

import java.util.List;

/** BLS aggregate public keys case */
public class BlsAggregatePubKeysCase implements TestCase {
  private List<String> input;
  private String output;

  public List<String> getInput() {
    return input;
  }

  public void setInput(List<String> input) {
    this.input = input;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  @Override
  public String toString() {
    return "Aggregate public keys {"
        + "input="
        + input.get(0).substring(0, 18)
        + "...(total: "
        + input.size()
        + " public keys)"
        + '}';
  }
}

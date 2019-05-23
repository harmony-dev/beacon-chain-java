package org.ethereum.beacon.test.type.bls;

import org.ethereum.beacon.test.type.TestCase;

/** BLS private to public test case */
public class BlsPrivateToPublicCase implements TestCase {
  private String input;
  private String output;

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
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
    return "Private to public key with input: " + input;
  }
}

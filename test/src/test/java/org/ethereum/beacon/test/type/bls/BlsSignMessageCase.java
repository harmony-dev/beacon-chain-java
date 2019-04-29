package org.ethereum.beacon.test.type.bls;

import org.ethereum.beacon.test.type.TestCase;

/** BLS sign message test case */
public class BlsSignMessageCase implements TestCase {
  private BlsSignMessageInput input;
  private String output;

  public BlsSignMessageInput getInput() {
    return input;
  }

  public void setInput(BlsSignMessageInput input) {
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
    return "Sign Message Test Case{" + "input=" + input + '}';
  }

  public static class BlsSignMessageInput extends BlsMessageHashCase.BlsMessageHashInput {
    private String privkey;

    public String getPrivkey() {
      return privkey;
    }

    public void setPrivkey(String privkey) {
      this.privkey = privkey;
    }
  }
}

package org.ethereum.beacon.test.type.bls;

import org.ethereum.beacon.test.type.TestCase;

import java.util.List;

/** BLS message hash compressed test case */
public class BlsMessageHashCompressedCase implements TestCase {
  private BlsMessageHashCase.BlsMessageHashInput input;
  private List<String> output;

  public BlsMessageHashCase.BlsMessageHashInput getInput() {
    return input;
  }

  public void setInput(BlsMessageHashCase.BlsMessageHashInput input) {
    this.input = input;
  }

  public List<String> getOutput() {
    return output;
  }

  public void setOutput(List<String> output) {
    this.output = output;
  }

  @Override
  public String toString() {
    return "Message hash as a compressed G2 point with input: " + input;
  }
}

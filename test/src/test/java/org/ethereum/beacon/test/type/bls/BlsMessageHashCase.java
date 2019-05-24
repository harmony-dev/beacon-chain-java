package org.ethereum.beacon.test.type.bls;

import org.ethereum.beacon.test.type.TestCase;

import java.util.List;

/** BLS message hash uncompressed test case */
public class BlsMessageHashCase implements TestCase {
  private BlsMessageHashInput input;
  private List<List<String>> output;

  public BlsMessageHashInput getInput() {
    return input;
  }

  public void setInput(BlsMessageHashInput input) {
    this.input = input;
  }

  public List<List<String>> getOutput() {
    return output;
  }

  public void setOutput(List<List<String>> output) {
    this.output = output;
  }

  @Override
  public String toString() {
    return "Message hash as a G2 point (uncompressed) with input: " + input;
  }

  public static class BlsMessageHashInput {
    private String domain;
    private String message;

    public String getDomain() {
      return domain;
    }

    public void setDomain(String domain) {
      this.domain = domain;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    @Override
    public String toString() {
      return "Input{" + "domain='" + domain + '\'' + ", message='" + message + '\'' + '}';
    }
  }
}

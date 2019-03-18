package org.ethereum.beacon.test.type;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Container for BLS tests <a
 * href="https://github.com/ethereum/eth2.0-test-generators/tree/master/bls/">https://github.com/ethereum/eth2.0-test-generators/tree/master/bls/</a>
 */
public class BlsTest extends TestSkeleton {
  @JsonProperty("case01_message_hash_G2_uncompressed")
  private List<BlsMessageHashCase> blsMessageHashCases;

  @JsonProperty("case02_message_hash_G2_compressed")
  private List<BlsMessageHashCompressedCase> blsMessageHashCompressedCases;

  @JsonProperty("case03_private_to_public_key")
  private List<BlsPrivateToPublicCase> blsPrivateToPublicCases;

  @JsonProperty("case04_sign_messages")
  private List<BlsSignMessageCase> blsSignMessageCases;
  // XXX: 05 is too slow and is not a part of standard tests therefore
  @JsonProperty("case06_aggregate_sigs")
  private List<BlsAggregateSigsCase> blsAggregateSigsCases;

  @JsonProperty("case07_aggregate_pubkeys")
  private List<BlsAggregatePubKeysCase> blsAggregatePubKeysCases;

  public UniversalTest<BlsMessageHashCase> buildBlsMessageHashTest() {
    UniversalTest test = new UniversalTest<BlsMessageHashCase>();
    test.setFork(this.getFork());
    test.setSummary(this.getSummary());
    test.setTestSuite(this.getTestSuite());
    test.setTitle(this.getTitle());
    test.setTestCases(getBlsMessageHashCases());

    return test;
  }

  public UniversalTest<BlsMessageHashCompressedCase> buildBlsMessageHashCompressedTest() {
    UniversalTest test = new UniversalTest<BlsMessageHashCompressedCase>();
    test.setFork(this.getFork());
    test.setSummary(this.getSummary());
    test.setTestSuite(this.getTestSuite());
    test.setTitle(this.getTitle());
    test.setTestCases(getBlsMessageHashCompressedCases());

    return test;
  }

  public UniversalTest<BlsPrivateToPublicCase> buildBlsPrivateToPublicTest() {
    UniversalTest test = new UniversalTest<BlsPrivateToPublicCase>();
    test.setFork(this.getFork());
    test.setSummary(this.getSummary());
    test.setTestSuite(this.getTestSuite());
    test.setTitle(this.getTitle());
    test.setTestCases(getBlsPrivateToPublicCases());

    return test;
  }

  public UniversalTest<BlsSignMessageCase> buildBlsSignMessageTest() {
    UniversalTest test = new UniversalTest<BlsSignMessageCase>();
    test.setFork(this.getFork());
    test.setSummary(this.getSummary());
    test.setTestSuite(this.getTestSuite());
    test.setTitle(this.getTitle());
    test.setTestCases(getBlsSignMessageCases());

    return test;
  }

  public UniversalTest<BlsAggregateSigsCase> buildBlsAggregateSigsTest() {
    UniversalTest test = new UniversalTest<BlsAggregateSigsCase>();
    test.setFork(this.getFork());
    test.setSummary(this.getSummary());
    test.setTestSuite(this.getTestSuite());
    test.setTitle(this.getTitle());
    test.setTestCases(getBlsAggregateSigsCases());

    return test;
  }

  public UniversalTest<BlsAggregatePubKeysCase> buildBlsAggregatePubKeysTest() {
    UniversalTest test = new UniversalTest<BlsAggregatePubKeysCase>();
    test.setFork(this.getFork());
    test.setSummary(this.getSummary());
    test.setTestSuite(this.getTestSuite());
    test.setTitle(this.getTitle());
    test.setTestCases(getBlsAggregatePubKeysCases());

    return test;
  }

  public List<BlsMessageHashCase> getBlsMessageHashCases() {
    return blsMessageHashCases;
  }

  public void setBlsMessageHashCases(List<BlsMessageHashCase> blsMessageHashCases) {
    this.blsMessageHashCases = blsMessageHashCases;
  }

  public List<BlsMessageHashCompressedCase> getBlsMessageHashCompressedCases() {
    return blsMessageHashCompressedCases;
  }

  public void setBlsMessageHashCompressedCases(
      List<BlsMessageHashCompressedCase> blsMessageHashCompressedCases) {
    this.blsMessageHashCompressedCases = blsMessageHashCompressedCases;
  }

  public List<BlsPrivateToPublicCase> getBlsPrivateToPublicCases() {
    return blsPrivateToPublicCases;
  }

  public void setBlsPrivateToPublicCases(List<BlsPrivateToPublicCase> blsPrivateToPublicCases) {
    this.blsPrivateToPublicCases = blsPrivateToPublicCases;
  }

  public List<BlsSignMessageCase> getBlsSignMessageCases() {
    return blsSignMessageCases;
  }

  public void setBlsSignMessageCases(List<BlsSignMessageCase> blsSignMessageCases) {
    this.blsSignMessageCases = blsSignMessageCases;
  }

  public List<BlsAggregateSigsCase> getBlsAggregateSigsCases() {
    return blsAggregateSigsCases;
  }

  public void setBlsAggregateSigsCases(List<BlsAggregateSigsCase> blsAggregateSigsCases) {
    this.blsAggregateSigsCases = blsAggregateSigsCases;
  }

  public List<BlsAggregatePubKeysCase> getBlsAggregatePubKeysCases() {
    return blsAggregatePubKeysCases;
  }

  public void setBlsAggregatePubKeysCases(List<BlsAggregatePubKeysCase> blsAggregatePubKeysCases) {
    this.blsAggregatePubKeysCases = blsAggregatePubKeysCases;
  }

  public static class BlsMessageHashCase implements TestCase {
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
  }

  public static class BlsMessageHashCompressedCase implements TestCase {
    private BlsMessageHashInput input;
    private List<String> output;

    public BlsMessageHashInput getInput() {
      return input;
    }

    public void setInput(BlsMessageHashInput input) {
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

  public static class BlsPrivateToPublicCase implements TestCase {
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

  public static class BlsSignMessageCase implements TestCase {
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

    public static class BlsSignMessageInput extends BlsMessageHashInput {
      private String privkey;

      public String getPrivkey() {
        return privkey;
      }

      public void setPrivkey(String privkey) {
        this.privkey = privkey;
      }
    }
  }

  public static class BlsAggregateSigsCase implements TestCase {
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
      return "Aggregate signatures {"
          + "input="
          + input.get(0).substring(0, 18)
          + "...(total: "
          + input.size()
          + " signatures)"
          + '}';
    }
  }

  public static class BlsAggregatePubKeysCase implements TestCase {
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
}

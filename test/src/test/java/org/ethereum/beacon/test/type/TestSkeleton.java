package org.ethereum.beacon.test.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Non-specific test from <a
 * href="https://github.com/ethereum/eth2.0-tests/">https://github.com/ethereum/eth2.0-tests/</a> -
 * Eth2.0 community tests repository. Following fields, or a bit less are in any test.
 *
 * <p>Specific test cases are made by extension of this class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TestSkeleton {
  private String title;
  private String summary;
  private String version;
  @JsonProperty("test_suite")
  private String testSuite;
  private String fork;
  @JsonProperty("test_cases")
  List<TestCase> testCases;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getTestSuite() {
    return testSuite;
  }

  public void setTestSuite(String testSuite) {
    this.testSuite = testSuite;
  }

  public String getFork() {
    return fork;
  }

  public void setFork(String fork) {
    this.fork = fork;
  }

  public List<TestCase> getTestCases() {
    return testCases;
  }

  @Override
  public String toString() {
    return "Test \"" + title + " v" + version + '\"';
  }
}

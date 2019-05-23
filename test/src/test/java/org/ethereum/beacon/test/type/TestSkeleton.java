package org.ethereum.beacon.test.type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Common test suite format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats#test-suite">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats#test-suite</a>
 *
 * <p>Specific test cases are made by extension of this class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TestSkeleton {
  @JsonProperty("test_cases")
  protected List<TestCase> testCases;
  private String title;
  private String summary;
  @JsonProperty("forks_timeline")
  private String forksTimeline;
  private List<String> forks;
  private String config;
  private String runner;
  private String handler;

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

  public String getForksTimeline() {
    return forksTimeline;
  }

  public void setForksTimeline(String forksTimeline) {
    this.forksTimeline = forksTimeline;
  }

  public List<String> getForks() {
    return forks;
  }

  public void setForks(List<String> forks) {
    this.forks = forks;
  }

  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
  }

  public String getRunner() {
    return runner;
  }

  public void setRunner(String runner) {
    this.runner = runner;
  }

  public String getHandler() {
    return handler;
  }

  public void setHandler(String handler) {
    this.handler = handler;
  }

  public List<TestCase> getTestCases() {
    return testCases;
  }

  @Override
  public String toString() {
    return "Test \"" + title + " [" + String.join(",", forks) + "]\"";
  }
}

package org.ethereum.beacon.test.type;

import java.util.List;

public class SSZ {
  private String title;
  private String summary;
  private String version;
  List<SSZTestCase> test_cases;

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

  public List<SSZTestCase> getTest_cases() {
    return test_cases;
  }

  public void setTest_cases(List<SSZTestCase> test_cases) {
    this.test_cases = test_cases;
  }

  public static class SSZTestCase {
    private String type;
    private boolean valid;
    private String value;
    private String ssz;
    private List<String> tags;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public boolean isValid() {
      return valid;
    }

    public void setValid(boolean valid) {
      this.valid = valid;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getSsz() {
      return ssz;
    }

    public void setSsz(String ssz) {
      this.ssz = ssz;
    }

    public List<String> getTags() {
      return tags;
    }

    public void setTags(List<String> tags) {
      this.tags = tags;
    }
  }
}

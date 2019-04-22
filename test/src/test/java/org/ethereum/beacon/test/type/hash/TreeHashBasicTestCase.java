package org.ethereum.beacon.test.type.hash;

import org.ethereum.beacon.test.type.TestCase;

import java.util.List;

/** Tree hash test case with basic value like number */
public class TreeHashBasicTestCase implements TestCase {
  private String type;
  private String root;
  private List<String> tags;
  private String value;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
